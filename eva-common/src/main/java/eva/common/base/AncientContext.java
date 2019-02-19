package eva.common.base;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eva.common.annotation.EvaEndpoint;
import eva.common.annotation.EvaService;
import eva.common.base.config.ServerConfig;
import eva.common.dto.ServiceMetadata;
import eva.common.exception.EvaAPIException;
import eva.common.util.ContextUtil;
import io.netty.util.concurrent.DefaultThreadFactory;

public class AncientContext extends AbstractContext<ServerConfig> implements BaseContext {

	private static final Logger LOG = LoggerFactory.getLogger(AncientContext.class);
	
	private static volatile Map<Class<?>, Object> BEANS = Maps.newConcurrentMap();

	private static volatile Set<ServiceMetadata> SERVICE_METADATA = Sets.newConcurrentHashSet();

	public AncientContext(ServerConfig config) throws Throwable {
		super(config);
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
		String[] scanPackages = config.getScanPackages();
			Set<Class<?>> allClasses = null;
			try {
				allClasses = ContextUtil.scanPackage(scanPackages);
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}
			if (Objects.isNull(allClasses)) {
				
				return;
			}
			for (Class<?> clazz : allClasses) {
				EvaService service = clazz.getAnnotation(EvaService.class);
				if (Objects.nonNull(service)) {
					Class<?> interfaceClass = service.serviceInterface();
					try {
						if (Objects.isNull(BEANS.get(interfaceClass))) {
							synchronized (BEANS) {
								if (Objects.isNull(BEANS.get(interfaceClass))) {
									Object proxyInstance = new JdkProxy(clazz.newInstance(), interfaceClass, LOADER)
											.getProxy();
									BEANS.put(interfaceClass, proxyInstance);
									ServiceMetadata smd = new ServiceMetadata();
									smd.setServiceClass(interfaceClass);
									Method[] methods = clazz.getDeclaredMethods();
									if (Objects.nonNull(methods)) {
										List<String> methodInfos = Lists.newArrayList();
										StringBuffer buffer = null;
										for (Method m : methods) {
											if (Objects.nonNull(m.getAnnotation(EvaEndpoint.class))) {
												String methodName = m.getName();
												buffer = new StringBuffer(methodName);
												for (Class<?> c : m.getParameterTypes()) {
													buffer.append(", " + c.getSimpleName());
												}
												methodInfos.add(buffer.toString());
											}
										}
										smd.setMethodInfos(methodInfos);
									}
									SERVICE_METADATA.add(smd);
								}
							}
						}
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
	}

	private static final class JdkProxy extends BaseProxy {

		private Semaphore sem;
		private Map<Method, Semaphore> methodSemaphoreMap = Maps.newConcurrentMap();
		private EvaService es;
		
		protected JdkProxy(Object target, Class<?> interfaceClass, ClassLoader classLoader) {
			super(target, interfaceClass, classLoader);
		}

		@Override
		protected Object getProxy() {
			return Proxy.newProxyInstance(classLoader, new Class<?>[] { interfaceClass }, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					EvaEndpoint endpoint = method.getAnnotation(EvaEndpoint.class);
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
						ExecutorService exe = Executors.newCachedThreadPool(new DefaultThreadFactory(target.getClass()) {
							@Override
							public Thread newThread(Runnable r) {
								final Thread thread = Executors.defaultThreadFactory().newThread(r);
//								thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
//									@Override
//									public void uncaughtException(Thread t, Throwable e) {
//										callFallback(fallback, args);
//									}
//								});
								return thread;
							}
						});
						Object result = null;
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
							try {
								if (usingOne.tryAcquire(semTimeout, semTimeUnit)) {
									f = exe.submit(() -> {
										try {
											method.invoke(target, args);
										} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
											e.printStackTrace();
											flag.set(false);
										}
									});
								} else {
									flag.set(false);
								}
							} finally {
								usingOne.release();
							}
						} else {
							f = exe.submit(() -> {
								try {
									method.invoke(target, args);
								} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
									e.printStackTrace();
									flag.set(false);
								}
							});
						}
						try {
							f.get(timeout, timeUnit);	
						} catch(Exception e) {
							e.printStackTrace();
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
				switch(strategy) {
				case EvaEndpoint.FALLBACK_FAIL_FAST: 
					throw new EvaAPIException("Call method [" + fallbackName + "] failed!");
				case EvaEndpoint.FALLBACK_RETRY: 
					for (;retryTime-- > 0;) {
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
	
}
