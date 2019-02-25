package eva.client.core.context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;

import eva.client.core.dto.ClientWrapper;
import eva.client.core.dto.SpecifiedConfig;
import eva.common.global.RequestID;
import eva.core.annotation.Fallback;
import eva.core.base.AbstractContext;
import eva.core.transport.Packet;
import eva.core.transport.Response;

public class Eva extends AbstractContext {

	private static final Cache<Long, ResponseFuture<Response>> TEMP_FUTURE = CacheBuilder.newBuilder()
			.expireAfterWrite(60 * 1000, TimeUnit.MILLISECONDS).maximumSize(8 * 1024).build();

	private static final Map<Class<?>, Object> PROXIES = Maps.newConcurrentMap();

	public static final <T> T getService(Class<T> interfaceClass) {
		return getService(interfaceClass, null);
	}

	@SuppressWarnings("unchecked")
	public static final <T> T getService(Class<T> interfaceClass, SpecifiedConfig config) {
		if (Objects.nonNull(PROXIES.get(interfaceClass))) {
			return (T) PROXIES.get(interfaceClass);
		} else {
			T proxy = (T) Proxy.newProxyInstance(LOADER, new Class<?>[] { interfaceClass }, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					String methodName = method.getName();
					Class<?>[] argTypes = method.getParameterTypes();
					Object[] methodArgs = args;
					long requestId = RequestID.getInstance().get();
					ResponseFuture<Response> f = new ResponseFuture<Response>();
					f.setRequestId(requestId);
					TEMP_FUTURE.put(requestId, f);
					System.out.println("put ID=" + requestId + ">>>>>>>>" + TEMP_FUTURE.stats());
					Packet p = new Packet();
					p.setArgs(methodArgs);
					p.setArgTypes(argTypes);
					p.setInterfaceClass(interfaceClass);
					p.setMethodName(methodName);
					p.setRequestId(requestId);
					ClientWrapper wrapper = ClientProvider.get().getSource(interfaceClass);
					long timeout = ClientProvider.get().getGlobalTimeoutMillSec();
					Object fallbackObj = null;
					if (Objects.nonNull(config) && config.getTimeout() > 0) {
						timeout = config.getTimeoutUnit().toMillis(config.getTimeout());
						fallbackObj = config.getFallback();
					}
					try {
						wrapper.getChannel().writeAndFlush(p);
						System.out.println("Invoke ID=" + requestId + "<<<<<<<<<<<<<<<<<<<<<");
						Response response = f.get(timeout, TimeUnit.MILLISECONDS);
						return response.getResult();
					} catch (Exception e) {
						if (Objects.isNull(fallbackObj)) {
							throw e;
						}
						if (Stream.of(fallbackObj.getClass().getInterfaces()).anyMatch(new Predicate<Class<?>>() {
							@Override
							public boolean test(Class<?> t) {
								return t == interfaceClass;
							}
						})) {
							Method fallbackMethod = fallbackObj.getClass().getDeclaredMethod(method.getName(),
									method.getParameterTypes());
							if (Objects.nonNull(fallbackMethod.getAnnotation(Fallback.class))) {
								return fallbackMethod.invoke(fallbackObj, args);
							} else {
								throw e;
							}
						} else {
							throw new Exception("Fallback instance is not an implementation of " + interfaceClass);
						}
					} finally {
						ClientProvider.get().putback(wrapper);
						TEMP_FUTURE.invalidate(requestId);
					}
				}
			});
			PROXIES.put(interfaceClass, proxy);
			return proxy;
		}

	}

	static final class ResponseFuture<V> implements Future<V> {

		private long requestId;

		private volatile V result;

		private CountDownLatch cdl = new CountDownLatch(1);

		@Override
		public boolean cancel(boolean arg0) {
			return false;
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public V get(long arg0, TimeUnit arg2) throws InterruptedException, ExecutionException, TimeoutException {
			cdl = new CountDownLatch(1);
			cdl.await(arg0, arg2);
			if (Objects.isNull(result)) {
				throw new TimeoutException("Timeout, while waiting for the result. Request ID: " + requestId);
			}
			return result;
		}

		@Override
		public boolean isCancelled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isDone() {
			// TODO Auto-generated method stub
			return false;
		}

		public void setResult(V response) {
			this.result = response;
			cdl.countDown();
		}

		public long getRequestId() {
			return requestId;
		}

		public void setRequestId(long requestId) {
			this.requestId = requestId;
		}

	}

	static final ResponseFuture<Response> getFuture(long requestId) {
		ResponseFuture<Response> f = TEMP_FUTURE.getIfPresent(requestId);
		if (Objects.nonNull(f)) {
			return f;
		}
		return null;
	}

}
