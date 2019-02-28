package eva.server.core.context;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Observable;
import java.util.Set;
import java.util.logging.Logger;

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
import com.google.common.collect.Sets;

import eva.common.global.ProviderMetadata;
import eva.common.global.StatusEvent;
import eva.common.registry.Registry;
import eva.core.annotation.EvaService;
import eva.core.base.BaseApplicationContext;
import eva.core.base.BaseContext;
import eva.core.base.config.ServerConfig;
import eva.core.exception.EvaContextException;
import eva.core.listener.StatusListener;
import eva.server.core.server.NioServer;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class AncientContext implements BaseContext<ConfigurableApplicationContext>, BaseApplicationContext, ApplicationContextAware {
	private static final Logger LOG = Logger.getLogger("AncientContext");
	private volatile Map<Class<?>, Object> EVA_BEANS = Maps.newConcurrentMap();
	private volatile Map<String, Object> DELAY_BEANS = Maps.newConcurrentMap();
	public static NioServer SERVER = null;
	private ServerConfig config;
	private ProviderMetadata PROVIDER_METADATA = new ProviderMetadata();

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
	public void init(ConfigurableApplicationContext context) throws EvaContextException {
		if (Objects.isNull(SERVER)) {
			synchronized (this) {
				if (Objects.isNull(config.getContext())) {
					config.setContext(context);
				}
				DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
				boolean needToRegister = Objects.nonNull(config.getRegistryAddress()) && !config.getRegistryAddress().trim().isEmpty();
				if (Objects.isNull(SERVER)) {
					SERVER = new NioServer(config, PROVIDER_METADATA);
				}
				if (Objects.nonNull(beanFactory)) {
					Map<String, Object> evaBeans = beanFactory.getBeansWithAnnotation(EvaService.class);
					replaceSpringBean(evaBeans, config.isInheritedInjection(), context);
					if (DELAY_BEANS.size() > 0) {
						replaceSpringBean(DELAY_BEANS, false, context);
					}
//					NioServer server = new NioServer(config, PROVIDER_METADATA);
					SERVER.addObserver(new StatusListener() {
						@Override
						public void onSuccess(Observable source, StatusEvent event) {
							try {
								Thread.sleep(500L);
								LOG.info("Registering local server to service registry");
							} catch (InterruptedException e) {
								LOG.info("Delay register eva server to registry failed, skip.");
							}
							// TODO register local host to registry
							Registry.get().addObserver(new StatusListener() {
								@Override
								public void onSuccess(Observable source, StatusEvent event) {
									LOG.info("Eva has beean registered on the registry");
								}
								@Override
								public void onFailure(Observable source, StatusEvent event) {
									LOG.info("Cannot register Eva to registry, RPC is unavailable.");
								}
							});
							try {
								Registry.get().registerServerToRegistry(config.getRegistryAddress(), PROVIDER_METADATA);
							} catch (IOException e1) {
								LOG.warning("Cannot register Eva to registry, RPC is unavailable. " + e1.getMessage());
							}
							String registryAddress = config.getRegistryAddress();
							if (Objects.isNull(registryAddress) || "".equals(registryAddress.trim())) {
								LOG.info("No registry address is provided, eva is cannot provide RPC service.");
							} else {
								Registry.get().addObserver(new StatusListener() {
									@Override
									public void onSuccess(Observable source, StatusEvent event) {
										LOG.info("Provider [" + config.getServerId() + "] registered!");
									}

									@Override
									public void onFailure(Observable source, StatusEvent event) {
										Throwable e = event.getExc();
										LOG.info("Failed to register provider: " + e.getMessage());
									}
								});
							}
						}

						@Override
						public void onFailure(Observable source, StatusEvent event) {
							LOG.warning("Eva encountered an error, cannot provide RPC service any more.");
						}

						@Override
						public void onClose(Observable source, StatusEvent event) {
							LOG.warning("Eva is shutted down, cannot provide RPC service any more.");
						}
					});
					if (needToRegister) {
						if (Objects.nonNull(EVA_BEANS) && !EVA_BEANS.isEmpty()) {
							Set<String> serviceNames = Sets.newHashSet();
							Set<Class<?>> set = EVA_BEANS.keySet();
							set.stream().forEach(c -> {
								serviceNames.add(c.getName());
							});
							PROVIDER_METADATA.setServices(serviceNames);
						}
					}
					SERVER.start();
				}
			}
		}

	}

	private void replaceSpringBean(Map<String, Object> beanMap, boolean processDelayBeans, ConfigurableApplicationContext context) throws EvaContextException {
		boolean success = true;
		Exception exc = null;
		DefaultListableBeanFactory bf = (DefaultListableBeanFactory) context.getBeanFactory();
		if (Objects.nonNull(beanMap) && beanMap.size() > 0) {
			Set<Entry<String, Object>> entries = beanMap.entrySet();
			EvaService service = null;
			BeanDefinitionBuilder builder = null;
			for (Entry<String, Object> entry : entries) {
				Object bean = entry.getValue();
				String beanName = entry.getKey();
				if (processDelayBeans && needDelay(beanName, bean, context)) {
					continue;
				}
				service = bean.getClass().getAnnotation(EvaService.class);
				Class<?> interfaceClass = service.interfaceClass();
				bf.destroyBean(bean);
				try {
					injectForEva(bean, context);
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

	private boolean needDelay(String beanName, Object bean, ConfigurableApplicationContext context) {
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
				Object fieldBean = context.getBean(interfaceClass);
				EvaService eva = fieldBean.getClass().getAnnotation(EvaService.class);
				if (Objects.nonNull(eva)) {
					DELAY_BEANS.put(beanName, bean);
					return true;
				}
			}
		}
		return false;
	}

	private void injectForEva(Object rowBean, ConfigurableApplicationContext context)
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
				Object fieldBean = context.getBean(interfaceClass);
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
				return (T) new JdkProxy(target, interfaceClass, ProxyFactoryBean.class.getClassLoader()).getProxy();
			} else {
				return (T) new CglibProxy(target, interfaceClass, ProxyFactoryBean.class.getClassLoader()).getProxy();
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
		ConfigurableApplicationContext context = (ConfigurableApplicationContext) ctx;
		try {
			init(context);
		} catch (EvaContextException e) {
			LOG.warning(e.getMessage());
		}
	}

	@Override
	public ServerConfig getServerConfig() {
		return config;
	}

}
