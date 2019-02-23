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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;

import eva.client.core.dto.ClientWrap;
import eva.common.global.RequestID;
import eva.core.annotation.EvaCall;
import eva.core.transport.Packet;
import eva.core.transport.Response;

public class Eva {

	private static final Cache<Long, ResponseFuture<Response>> TEMP_FUTURE = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS).maximumSize(8192)
            .build();

	private static ClassLoader LOADER = null;

	static {
		LOADER = Thread.currentThread().getContextClassLoader();
		if (Objects.isNull(LOADER)) {
			LOADER = Eva.class.getClassLoader();
		}
	}

	private static final Map<Class<?>, Object> PROXIES = Maps.newConcurrentMap();

	@SuppressWarnings("unchecked")
	public static final <T> T getService(Class<T> interfaceClass) {
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
					TEMP_FUTURE.put(requestId, f);
					System.out.println("put ID=" + requestId + ">>>>>>>>" + TEMP_FUTURE.stats());
					Packet p = new Packet();
					p.setArgs(methodArgs);
					p.setArgTypes(argTypes);
					p.setInterfaceClass(interfaceClass);
					p.setMethodName(methodName);
					p.setRequestId(requestId);
					ClientWrap wrap = ClientProvider.get().getSource(interfaceClass);
					long timeout = ClientProvider.get().getGlobalTimeoutMillSec();
					EvaCall call = interfaceClass.getAnnotation(EvaCall.class);
					if (Objects.nonNull(call)) {
						int timeoutVal = call.timeout();
						timeout = call.timeUnit().toMillis(timeoutVal);
					}
					try {
						wrap.getChannel().writeAndFlush(p);
						Response response = f.get(timeout, TimeUnit.MILLISECONDS);
						return response.getResult();
					} catch (Exception e) {
						throw e;
					} finally {
						ClientProvider.get().putback(wrap);
					}
				}
			});
			PROXIES.put(interfaceClass, proxy);
			return proxy;
		}
		
	}

	static final class ResponseFuture<V> implements Future<V> {

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
			cdl.await(arg0, arg2);
			if (Objects.isNull(result)) {
				throw new TimeoutException("Timeout, while waiting for the result.");
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
		
	}
	
	static final ResponseFuture<Response> getFuture(long requestId) {
		ResponseFuture<Response> f =  TEMP_FUTURE.getIfPresent(requestId);
		if (Objects.nonNull(f)) {
			TEMP_FUTURE.invalidate(f);
			return f;
		}
		return null;
	}

}
