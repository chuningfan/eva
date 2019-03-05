package eva.core.valve;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Maps;

import eva.core.annotation.EvaDoll;
import eva.core.annotation.EvaService;
import eva.core.dto.NoSemaphore;
import eva.core.dto.ReturnVoid;

public abstract class InvokerValve<T, R extends Result> extends Valve<T, R> {

	private static final ExecutorService INVOKER_THREADS = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
	
	protected static final Map<Class<?>, Semaphore> SERVICE_SEM = Maps.newConcurrentMap();

	protected Object processInWrap(Object source, Method method, Object... args) throws Exception {
		EvaService evaService = source.getClass().getAnnotation(EvaService.class);
		String returnType = method.getReturnType().getName();
		Object result = null;
		if (Objects.nonNull(evaService)) {
			long timeout = evaService.timeout();
			TimeUnit unit = evaService.timeUnit();
			EvaDoll evaDoll = method.getAnnotation(EvaDoll.class);
			if (Objects.nonNull(evaDoll)) {
				if (Objects.nonNull(evaDoll)) {
					if (evaDoll.timeout() > 0) {
						timeout = evaDoll.timeout();
						unit = evaDoll.timeUnit();
					}
				}
			}
			if (timeout > 0) {
				Future<Object> f = INVOKER_THREADS.submit(new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						return processInvoke(source, method, (evaDoll == null || evaDoll.fallback().trim().length() < 1) ? null : evaDoll.fallback().trim(), args);
					}
				});
				try {
					result = f.get(timeout, unit);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					if (e instanceof TimeoutException) {
						f.cancel(true);
					}
					throw e;
				}
			} else {
				result = processInvoke(source, method, (evaDoll == null || evaDoll.fallback().trim().length() < 1) ? null : evaDoll.fallback().trim(), args);
			}
		} else {
			result = processInvoke(source, method, null, args);
		}
		if ("void".equalsIgnoreCase(returnType)) {
			return ReturnVoid.getInstance();
		}
		return result;
	}

	private Object processInvoke(Object source, Method method, String fallbackName, Object... args) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object result = null;
		int pCount = method.getParameterCount();
		EvaService evaService = source.getClass().getAnnotation(EvaService.class);
		Semaphore sem = null;
		if (Objects.nonNull(evaService)) {
			Class<?> interfaceClass = evaService.interfaceClass();
			sem = SERVICE_SEM.get(interfaceClass);
		}
		if (Objects.nonNull(sem)) {
			if (sem.tryAcquire()) {
				try {
					if (pCount < 1) {
						result = method.invoke(source);
					} else {
						result = method.invoke(source, args);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					if (Objects.nonNull(fallbackName) && fallbackName.trim().length() > 0) {
						Method fallbackMethod = source.getClass().getMethod(fallbackName,method.getParameterTypes());
						return fallbackMethod.invoke(source, args);
					} else {
						throw e;
					}
				} finally {
					sem.release();
				}
			} else {
				result = NoSemaphore.getInstance();
			}
		} else {
			try {
				if (pCount < 1) {
					result = method.invoke(source);
				} else {
					result = method.invoke(source, args);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				if (Objects.nonNull(fallbackName) && fallbackName.trim().length() > 0) {
					Method fallbackMethod = source.getClass().getMethod(fallbackName,method.getParameterTypes());
					return fallbackMethod.invoke(source, args);
				} else {
					throw e;
				}
			}
		}
		return result;
	}

}
