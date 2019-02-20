package eva.server.core.context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
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
import eva.common.dto.ReturnVoid;
import eva.common.exception.EvaAPIException;
import eva.server.core.server.NioServer;
import io.netty.util.concurrent.DefaultThreadFactory;

public class AncientContext extends AbstractContext implements BaseContext, ApplicationContextAware {

	private static final Logger LOG = LoggerFactory.getLogger(AncientContext.class);

	private static volatile Map<Class<?>, Object> BEANS = Maps.newConcurrentMap();

	public static ConfigurableApplicationContext CONTEXT = null;

	private ServerConfig config;

	public AncientContext(ServerConfig config) throws Throwable {
		super();
		this.config = config;
	}

	@Override
	public void update(Observable arg0, Object arg1) {

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
	public void init() {
		DefaultListableBeanFactory bf = (DefaultListableBeanFactory) CONTEXT.getBeanFactory();
		Map<String, Object> evaBeans = bf.getBeansWithAnnotation(EvaService.class);
		if (Objects.nonNull(evaBeans) && evaBeans.size() > 0) {
			Set<Entry<String, Object>> entries = evaBeans.entrySet();
			EvaService service = null;
			BeanDefinitionBuilder builder = null;
			for (Entry<String, Object> entry : entries) {
				Object bean = entry.getValue();
				String beanName = entry.getKey();
				service = bean.getClass().getAnnotation(EvaService.class);
				Class<?> interfaceClass = service.interfaceClass();
				bf.destroyBean(bean);
				builder = BeanDefinitionBuilder.genericBeanDefinition(interfaceClass);
				GenericBeanDefinition beanDef = (GenericBeanDefinition) builder.getRawBeanDefinition();
				beanDef.getPropertyValues().add("interfaceClass", interfaceClass);
				beanDef.getPropertyValues().add("target", bean);
				beanDef.setBeanClass(ProxyFactoryBean.class);
				// ConstructorArgumentValues constructorArgs = new
				// ConstructorArgumentValues();
				// beanDef.setConstructorArgumentValues(constructorArgs);
				beanDef.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
				bf.registerBeanDefinition(beanName, beanDef);
			}
		}
		// for (Class<?> clazz : allClasses) {
		// EvaService service = clazz.getAnnotation(EvaService.class);
		// if (Objects.nonNull(service)) {
		// Class<?> beanType = service.springBeanType();
		// Class<?> interfaceClass = service.interfaceClass();
		// try {
		//
		// Object bean = bf.getBean(beanType);
		// bf.destroyBean(bean);
		// Object proxyInstance = new JdkProxy(clazz.newInstance(),
		// interfaceClass, LOADER)
		// .getProxy();
		// bf.registerSingleton(arg0, arg1);
		// bf.
		// } catch (InstantiationException | IllegalAccessException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		NioServer server = new NioServer(config);
		server.start();
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
			return (T) new JdkProxy(target, interfaceClass, LOADER).getProxy();
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

	private static final class JdkProxy extends BaseProxy {

		private Semaphore sem;
		private Map<Method, Semaphore> methodSemaphoreMap = Maps.newConcurrentMap();
		private EvaService es;

		protected JdkProxy(Object target, Class<?> interfaceClass, ClassLoader classLoader) {
			super(target, interfaceClass, classLoader);
			es = target.getClass().getAnnotation(EvaService.class);
			if (es.maximumConcurrency() > 0) {
				sem = new Semaphore(es.maximumConcurrency());
			}
		}

		@Override
		protected Object getProxy() {
			return Proxy.newProxyInstance(classLoader, new Class<?>[] { interfaceClass }, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					EvaEndpoint endpoint = target.getClass().getMethod(method.getName(), method.getParameterTypes())
							.getAnnotation(EvaEndpoint.class);
					if (Objects.nonNull(endpoint)) {
						String fallback = !"".equals(endpoint.fallback().trim()) ? endpoint.fallback().trim() : null;
						long timeout = endpoint.timeout();
						TimeUnit timeUnit = endpoint.timeUnit();
						int methodSemVal = endpoint.maximumConcurrency();
						int fallbackStrategy = endpoint.fallbackStrategy();
						int retryTime = endpoint.retryTime();
						Semaphore methodSemaphore = null;
						if (methodSemVal > 0) {
							if (Objects.isNull(methodSemaphoreMap.get(method))) {
								methodSemaphoreMap.put(method, new Semaphore(methodSemVal));
							}
							methodSemaphore = methodSemaphoreMap.get(method);
						}
						ExecutorService exe = Executors
								.newCachedThreadPool(new DefaultThreadFactory(target.getClass()) {
									@Override
									public Thread newThread(Runnable r) {
										final Thread thread = Executors.defaultThreadFactory().newThread(r);
										return thread;
									}
								});
						Object result = null;
						if ("void".equalsIgnoreCase(method.getReturnType().getName())) {
							result = ReturnVoid.getInstance();
						}
						Semaphore usingOne = null;
						int semTimeout = 0;
						TimeUnit semTimeUnit = null;
						if (Objects.isNull(methodSemaphore)) {
							if (Objects.nonNull(sem)) {
								usingOne = sem;
								semTimeout = es.acquireTimeout();
								semTimeUnit = es.acquireTimeUnit();
							}
						} else {
							usingOne = methodSemaphore;
							semTimeout = endpoint.acquireTimeout();
							semTimeUnit = endpoint.acquireTimeUnit();
						}
						AtomicBoolean flag = new AtomicBoolean(true);
						Future<?> f = null;
						if (Objects.nonNull(usingOne)) {
							if (usingOne.tryAcquire(semTimeout, semTimeUnit)) {
								f = exe.submit(() -> {
									try {
										method.invoke(target, args);
									} catch (IllegalAccessException | IllegalArgumentException
											| InvocationTargetException e) {
										e.printStackTrace();
										flag.set(false);
									}
								});
								usingOne.release();
							} else {
								flag.set(false);
							}
						} else {
							f = exe.submit(() -> {
								try {
									method.invoke(target, args);
								} catch (IllegalAccessException | IllegalArgumentException
										| InvocationTargetException e) {
									e.printStackTrace();
									flag.set(false);
								}
							});
						}
						try {
							if (flag.get()) {
								result = f.get(timeout, timeUnit);
							}
						} catch (Exception e) {
							e.printStackTrace();
							if (e instanceof TimeoutException) {
								f.cancel(true);
							}
							flag.set(false);
						} finally {
							exe.shutdown();
						}
						if (!flag.get()) {
							if (Objects.nonNull(fallback)) {
								callFallback(method, target, fallback, retryTime, fallbackStrategy, args);
							}
						}
						return result;
					} else {
						return method.invoke(target, args);
					}
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
		init();
	}

}
