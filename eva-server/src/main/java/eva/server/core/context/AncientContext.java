package eva.server.core.context;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Observable;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.common.collect.Maps;

import eva.core.annotation.EvaService;
import eva.core.base.AbstractContext;
import eva.core.base.BaseApplicationContext;
import eva.core.base.BaseContext;
import eva.core.base.config.ServerConfig;
import eva.core.dto.ProviderMetadata;
import eva.core.dto.StatusEvent;
import eva.core.exception.EvaContextException;
import eva.core.listener.StatusListener;
import eva.core.registry.Registry;
import eva.server.core.server.NioServer;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class AncientContext extends AbstractContext
		implements BaseContext, BaseApplicationContext, ApplicationContextAware {
	private static volatile Map<Class<?>, Object> EVA_BEANS = Maps.newConcurrentMap();
	private static volatile Map<String, Object> DELAY_BEANS = Maps.newConcurrentMap();
	public static NioServer SERVER = null;
	public static ConfigurableApplicationContext CONTEXT = null;
	private static DefaultListableBeanFactory BEAN_FACTORY = null;
	private ServerConfig config;
	private static ProviderMetadata PROVIDER_METADATA = new ProviderMetadata();

	public AncientContext(ServerConfig config) throws Throwable {
		this.config = config;
	}

	@Override
	public <T> T getBean(Class<T> beanClass) {
		return null;
	}

	@Override
	public void removeBean(Class<?> beanClass) {
		synchronized (EVA_BEANS) {
			EVA_BEANS.remove(beanClass);
		}
	}

	@Override
	public void init() throws EvaContextException {
		if (Objects.isNull(SERVER)) {
			synchronized (this) {
				if (Objects.isNull(SERVER)) {
					SERVER = new NioServer(config, PROVIDER_METADATA);
				}
				if (Objects.nonNull(BEAN_FACTORY)) {
					Map<String, Object> evaBeans = BEAN_FACTORY.getBeansWithAnnotation(EvaService.class);
					replaceSpringBean(evaBeans, config.isInheritedInjection());
					if (DELAY_BEANS.size() > 0) {
						replaceSpringBean(DELAY_BEANS, false);
					}
					NioServer server = new NioServer(config, PROVIDER_METADATA);
					server.addObserver(new StatusListener() {
						@Override
						public void onSuccess(Observable source, StatusEvent event) {
							try {
								Thread.sleep(500L);
								LOG.info("Registering local server to service registry");
							} catch (InterruptedException e) {
								e.printStackTrace();
								LOG.warn("Delay register eva server to registry failed, skip.");
							}
							// TODO register local host to registry
							String registryAddress = config.getRegistryAddress();
							if (Objects.isNull(registryAddress) || "".equals(registryAddress.trim())) {
								LOG.warn("No registry address is provided, eva is cannot provide RPC service.");
							} else {
								Registry.get().addObserver(new StatusListener() {
									@Override
									public void onSuccess(Observable source, StatusEvent event) {
										LOG.info("Provider [" + config.getServerId() + "] registered!");
									}

									@Override
									public void onFailure(Observable source, StatusEvent event) {
										Throwable e = event.getExc();
										LOG.error("Failed to register provider: " + e.getMessage());
									}
								});
							}
						}

						@Override
						public void onFailure(Observable source, StatusEvent event) {
							LOG.warn("Eva encountered an error, cannot provide RPC service any more.");
						}

						@Override
						public void onClose(Observable source, StatusEvent event) {
							LOG.warn("Eva is shutted down, cannot provide RPC service any more.");
						}
					});
					server.start();
				}
			}
		}

	}

	private void replaceSpringBean(Map<String, Object> beanMap, boolean processDelayBeans) throws EvaContextException {
		boolean success = true;
		Exception exc = null;
		DefaultListableBeanFactory bf = (DefaultListableBeanFactory) CONTEXT.getBeanFactory();
		if (Objects.nonNull(beanMap) && beanMap.size() > 0) {
			Set<Entry<String, Object>> entries = beanMap.entrySet();
			EvaService service = null;
			BeanDefinitionBuilder builder = null;
			for (Entry<String, Object> entry : entries) {
				Object bean = entry.getValue();
				String beanName = entry.getKey();
				if (processDelayBeans && needDelay(beanName, bean)) {
					continue;
				}
				service = bean.getClass().getAnnotation(EvaService.class);
				Class<?> interfaceClass = service.interfaceClass();
				bf.destroyBean(bean);
				try {
					injectForEva(bean);
				} catch (IllegalArgumentException | IllegalAccessException | ClassNotFoundException e) {
					exc = e;
					success = false;
					break;
				}
				if (interfaceClass == Object.class) {
					interfaceClass = bean.getClass();
				}
				builder = BeanDefinitionBuilder.genericBeanDefinition(interfaceClass);
				GenericBeanDefinition beanDef = (GenericBeanDefinition) builder.getRawBeanDefinition();
				beanDef.getPropertyValues().add("interfaceClass", interfaceClass);
				beanDef.getPropertyValues().add("target", bean);
				beanDef.setBeanClass(ProxyFactoryBean.class);
				beanDef.addQualifier(new AutowireCandidateQualifier(interfaceClass));
				beanDef.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
				bf.registerBeanDefinition(beanName, beanDef);
				EVA_BEANS.put(interfaceClass, bf.getBean(beanName));
			}
			if (!success) {
				throw new EvaContextException("When replacing bean in spring occurred an error: " + exc.getMessage());
			}
		}
	}

	private boolean needDelay(String beanName, Object bean) {
		Class<?> beanClass = bean.getClass();
		Field[] fields = beanClass.getDeclaredFields();
		if (Objects.isNull(fields) || fields.length == 0) {
			return false;
		}
		for (Field f : fields) {
			Autowired auto = f.getAnnotation(Autowired.class);
			if (auto == null) {
				continue;
			} else {
				Class<?> interfaceClass = f.getType();
				Object fieldBean = CONTEXT.getBean(interfaceClass);
				EvaService eva = fieldBean.getClass().getAnnotation(EvaService.class);
				if (Objects.nonNull(eva)) {
					DELAY_BEANS.put(beanName, bean);
					return true;
				}
			}
		}
		return false;
	}

	private void injectForEva(Object rowBean)
			throws IllegalArgumentException, IllegalAccessException, ClassNotFoundException {
		Class<?> beanClass = rowBean.getClass();
		Field[] fields = beanClass.getDeclaredFields();
		if (Objects.isNull(fields) || fields.length == 0) {
			return;
		}
		for (Field f : fields) {
			Autowired auto = f.getAnnotation(Autowired.class);
			if (auto == null) {
				continue;
			} else {
				Class<?> interfaceClass = f.getType();
				Object fieldBean = CONTEXT.getBean(interfaceClass);
				f.setAccessible(true);
				f.set(rowBean, fieldBean);
			}
		}
	}

	private static final class ProxyFactoryBean<T> implements FactoryBean<T> {
		private Class<T> interfaceClass;
		private Object target;

		@SuppressWarnings("unused")
		public Class<T> getInterfaceClass() {
			return interfaceClass;
		}

		@SuppressWarnings("unused")
		public void setInterfaceClass(Class<T> interfaceClass) {
			this.interfaceClass = interfaceClass;
		}

		@SuppressWarnings("unused")
		public Object getTarget() {
			return target;
		}

		@SuppressWarnings("unused")
		public void setTarget(Object target) {
			this.target = target;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T getObject() throws Exception {
			if (interfaceClass.isInterface()) {
				return (T) new JdkProxy(target, interfaceClass, LOADER).getProxy();
			} else {
				return (T) new CglibProxy(target, interfaceClass, LOADER).getProxy();
			}
		}

		@Override
		public Class<?> getObjectType() {
			return interfaceClass;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

	private static final class CglibProxy extends BaseProxy implements MethodInterceptor {
		protected CglibProxy(Object target, Class<?> interfaceClass, ClassLoader classLoader,
				Object... constructorArgs) {
			super(target, interfaceClass, classLoader, constructorArgs);
		}

		@Override
		protected Object getProxy() {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(this.target.getClass());
			enhancer.setCallback(this);
			return enhancer.create();
		}

		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			return invokeMethod(proxy, method, args);
		}
	}

	private static final class JdkProxy extends BaseProxy {
		protected JdkProxy(Object target, Class<?> interfaceClass, ClassLoader classLoader) {
			super(target, interfaceClass, classLoader);
		}

		@Override
		protected Object getProxy() {
			return Proxy.newProxyInstance(classLoader, new Class<?>[] { interfaceClass }, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return invokeMethod(proxy, method, args);
				}
			});
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		CONTEXT = (ConfigurableApplicationContext) ctx;
		BEAN_FACTORY = (DefaultListableBeanFactory) CONTEXT.getBeanFactory();
		try {
			init();
		} catch (EvaContextException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ServerConfig getServerConfig() {
		return config;
	}

}
