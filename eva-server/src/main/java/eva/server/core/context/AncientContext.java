package eva.server.core.context;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import eva.common.annotation.EvaEndpoint;
import eva.common.annotation.EvaService;
import eva.common.base.AbstractContext;
import eva.common.base.BaseContext;
import eva.common.base.config.ServerConfig;
import eva.common.exception.EvaAPIException;
import eva.common.exception.EvaContextException;
import eva.common.util.PacketUtil;
import eva.server.core.server.NioServer;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class AncientContext extends AbstractContext implements BaseContext, ApplicationContextAware {
	private static final Logger LOG = LoggerFactory.getLogger(AncientContext.class);
	private static volatile Map<Class<?>, Object> BEANS = Maps.newConcurrentMap();
	private static volatile Map<String, Object> DELAY_BEANS = Maps.newConcurrentMap();
	public static ConfigurableApplicationContext CONTEXT = null;
	private ServerConfig config;
	public AncientContext(ServerConfig config) throws Throwable {
		this.config = config;
	}
	@Override
	public <T> T getBean(Class<T> beanClass) {
		return null;
	}
	@Override
	public void removeBean(Class<?> beanClass) {
		synchronized (BEANS) {
			BEANS.remove(beanClass);
		}
	}
	@Override
	public void init() throws EvaContextException {
		DefaultListableBeanFactory bf = (DefaultListableBeanFactory) CONTEXT.getBeanFactory();
		Map<String, Object> evaBeans = bf.getBeansWithAnnotation(EvaService.class);
		replaceSpringBean(evaBeans, true);
		if (DELAY_BEANS.size() > 0) {
			replaceSpringBean(DELAY_BEANS, false);
		}
		NioServer server = new NioServer(config);
		server.start();
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
				BEANS.put(interfaceClass, bf.getBean(beanName));
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
			if (Objects.nonNull(constructorArgs) && constructorArgs.length > 0) {
				Class<?>[] constructorArgTypes = PacketUtil.getTypes(constructorArgs);
				return enhancer.create(constructorArgTypes, constructorArgs);
			}
			return enhancer.create();
		}
		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			return invokeMethod(proxy, method, args);
		}
		@Override
		protected Object callFallback(Method method, Object target, String fallbackName, int strategy, int retryTime,
				Object... args) throws EvaAPIException {
			return null;
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
		@Override
		protected Object callFallback(Method method, Object target, String fallbackName, int strategy, int retryTime,
				Object... args) throws EvaAPIException {
			if (Objects.isNull(fallbackName)) {
				return null;
			}
			Object res = null;
			try {
				Method fallbackMethod = target.getClass().getDeclaredMethod(fallbackName, method.getParameterTypes());
				switch (strategy) {
				case EvaEndpoint.FALLBACK_FAIL_FAST:
					throw new EvaAPIException("Call method [" + fallbackName + "] failed!");
				case EvaEndpoint.FALLBACK_RETRY:
					for (; retryTime-- > 0;) {
						try {
							Thread.sleep(500L);
							return method.invoke(target, args);
						} catch (Exception e) {
							LOG.error("Retrying call [" + method.getName() + "] but failed.");
						}
					}
					break;
				default:
					break;
				}
				res = fallbackMethod.invoke(target, args);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
			return res;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		CONTEXT = (ConfigurableApplicationContext) ctx;
		try {
			init();
		} catch (EvaContextException e) {
			e.printStackTrace();
		}
	}

}
