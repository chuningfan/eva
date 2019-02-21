package eva.common.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.Maps;

import eva.common.annotation.EvaEndpoint;
import eva.common.annotation.EvaService;
import eva.common.dto.ReturnVoid;
import eva.common.exception.EvaAPIException;
import eva.common.exception.EvaContextException;
import io.netty.util.concurrent.DefaultThreadFactory;

public interface BaseContext {

	<T> T getBean(Class<T> beanClass);

	void removeBean(Class<?> beanClass);

	void init() throws EvaContextException;

	public static abstract class BaseProxy {

		protected Object target;
		
		protected Class<?> interfaceClass;
		
		protected ClassLoader classLoader;
		
		protected Object[] constructorArgs;
		
		protected Semaphore sem;
		
		protected Map<Method, Semaphore> methodSemaphoreMap;
		
		protected EvaService es;

		protected BaseProxy(Object target, Class<?> interfaceClass, ClassLoader classLoader, Object...constructorArgs) {
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
		
		protected abstract Object getProxy();

		protected abstract Object callFallback(Method method, Object target, String fallbackName, int strategy,
				int retryTime, Object... args) throws EvaAPIException;

	}

}
