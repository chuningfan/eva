package eva.core.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.esotericsoftware.minlog.Log;
import com.google.common.collect.Maps;

import eva.core.annotation.EvaEndpoint;
import eva.core.annotation.EvaService;
import eva.core.base.config.ServerConfig;
import eva.core.dto.ReturnVoid;
import eva.core.exception.EvaAPIException;
import io.netty.util.concurrent.DefaultThreadFactory;

public interface BaseApplicationContext {
	
	ServerConfig getServerConfig();
	
	public static abstract class BaseProxy {

		protected Object target;

		protected Class<?> interfaceClass;

		protected ClassLoader classLoader;

		protected Object[] constructorArgs;

		protected Semaphore sem;

		protected Map<Method, Semaphore> methodSemaphoreMap;

		protected EvaService es;

		protected BaseProxy(Object target, Class<?> interfaceClass, ClassLoader classLoader,
				Object... constructorArgs) {
			this.target = target;

			this.interfaceClass = interfaceClass;

			this.classLoader = classLoader;

			this.constructorArgs = constructorArgs;

			this.methodSemaphoreMap = Maps.newConcurrentMap();

			es = target.getClass().getAnnotation(EvaService.class);
			if (es.maximumConcurrency() > 0) {
				sem = new Semaphore(es.maximumConcurrency());
			}
		}

		protected Object invokeMethod(Object proxy, Method method, Object[] args) throws Throwable {
			EvaEndpoint endpoint = target.getClass().getMethod(method.getName(), method.getParameterTypes())
					.getAnnotation(EvaEndpoint.class);
			Object result = null;
			String fallback = null;
			AtomicBoolean isFailed = new AtomicBoolean(false);
			String returnType = method.getReturnType().getName();
			if (Objects.nonNull(endpoint)) {
				fallback = !"".equals(endpoint.fallback().trim()) ? endpoint.fallback().trim() : null;
				long timeout = endpoint.timeout();
				TimeUnit timeUnit = endpoint.timeUnit();
				int methodSemVal = endpoint.maximumConcurrency();
				Semaphore methodSemaphore = null;
				if (methodSemVal > 0) {
					if (Objects.isNull(methodSemaphoreMap.get(method))) {
						methodSemaphoreMap.put(method, new Semaphore(methodSemVal));
					}
					methodSemaphore = methodSemaphoreMap.get(method);
				}
				Semaphore uniqueSem = null;
				Integer semTimeout = null;
				TimeUnit semTimeUnit = null;
				if (Objects.isNull(methodSemaphore)) {
					if (Objects.nonNull(sem)) {
						uniqueSem = sem;
						semTimeout = es.acquireTimeout() < 0 ? 3 : es.acquireTimeout();
						semTimeUnit = es.acquireTimeout() < 0 ? TimeUnit.SECONDS : es.acquireTimeUnit();
					}
				} else {
					uniqueSem = methodSemaphore;
					semTimeout = endpoint.acquireTimeout() < 0 ? 3 : endpoint.acquireTimeout();
					semTimeUnit = endpoint.acquireTimeout() < 0 ? TimeUnit.SECONDS : endpoint.acquireTimeUnit();
				}
				if (timeout > 0L) {
					Future<?> f = null;
					ExecutorService exe = Executors.newSingleThreadExecutor(new DefaultThreadFactory(target.getClass()) {
						@Override
						public Thread newThread(Runnable r) {
							final Thread thread = Executors.defaultThreadFactory().newThread(r);
							return thread;
						}
					});
					// use future to control timeout
					if (Objects.nonNull(uniqueSem)) {
						if (uniqueSem.tryAcquire(semTimeout, semTimeUnit)) {
							try {
								f = exe.submit(() -> {
									try {
										method.invoke(target, args);
									} catch (IllegalAccessException | IllegalArgumentException
											| InvocationTargetException e) {
										e.printStackTrace();
										isFailed.set(true);
									}
								});
								result = f.get(timeout, timeUnit);
							} catch (InterruptedException | ExecutionException | TimeoutException e) {
								if (e instanceof TimeoutException) {
									f.cancel(true);
								}
								isFailed.set(true);
							} finally {
								uniqueSem.release();
								exe.shutdown();
							}
						} else {
							Log.info("Access is limited!");
							isFailed.set(true);
						}
					} else {
						f = exe.submit(() -> {
							try {
								method.invoke(target, args);
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								e.printStackTrace();
								isFailed.set(true);
							}
						});
						try {
							result = f.get(timeout, timeUnit);
						} catch (InterruptedException | ExecutionException | TimeoutException e) {
							if (e instanceof TimeoutException) {
								f.cancel(true);
							}
							isFailed.set(true);
						} finally {
							exe.shutdown();
						}
					}
				} else {
					if (Objects.nonNull(uniqueSem)) {
						if (uniqueSem.tryAcquire(semTimeout, semTimeUnit)) {
							try {
								result = method.invoke(target, args);
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								e.printStackTrace();
								isFailed.set(true);
							} finally {
								uniqueSem.release();
							}
						}
					} else {
						try {
							result = method.invoke(target, args);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							e.printStackTrace();
							isFailed.set(true);
						}
					}
				}
			} else {
				try {
					result = method.invoke(target, args);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
					isFailed.set(true);
				}
			}
			if ("void".equalsIgnoreCase(returnType)) {
				result = ReturnVoid.getInstance();
			}
			if (isFailed.get()) {
				if (Objects.isNull(fallback)) {
					throw new EvaAPIException("");
				} else {
					result = callFallback(fallback, proxy, method, args);
				}
			}
			return result;
		}

		protected abstract Object getProxy();

		protected Object callFallback(String fallbackName, Object proxy, Method method, Object[] args) throws EvaAPIException {
			if (Objects.isNull(fallbackName)) {
				throw new EvaAPIException("No fallback method name!");
			}
			Object res = null;
			try {
				Method fallbackMethod = target.getClass().getDeclaredMethod(fallbackName, method.getParameterTypes());
				res = fallbackMethod.invoke(target, args);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
				throw new EvaAPIException(e.getMessage());
			}
			if ("void".equalsIgnoreCase(method.getReturnType().getName())) {
				res = ReturnVoid.getInstance();
			}
			return res;
		}

	}
	
}
