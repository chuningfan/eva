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

public class RPCClient {

	private static final ThreadLocal<ResponseFuture<Response>> LOCAL = new ThreadLocal<ResponseFuture<Response>>() {
		@Override
		protected ResponseFuture<Response> initialValue() {
			return new ResponseFuture<Response>();
		}
	};

	private static final Cache<Long, ResponseFuture<Response>> TEMP_FUTURE = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build();

	private static ClassLoader LOADER = null;

	static {
		LOADER = Thread.currentThread().getContextClassLoader();
		if (Objects.isNull(LOADER)) {
			LOADER = RPCClient.class.getClassLoader();
		}
	}

	private static final Map<Class<?>, Object> PROXIES = Maps.newConcurrentMap();

	public static final Object getService(Class<?> interfaceClass) {
		if (Objects.nonNull(PROXIES.get(interfaceClass))) {
			return PROXIES.get(interfaceClass);
		}
		return Proxy.newProxyInstance(LOADER, new Class<?>[] { interfaceClass }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				String methodName = method.getName();
				Class<?>[] argTypes = method.getParameterTypes();
				Object[] methodArgs = args;
				long requestId = RequestID.getInstance().get();
				ResponseFuture<Response> f = LOCAL.get();
				TEMP_FUTURE.put(requestId, f);
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
				wrap.getChannel().writeAndFlush(p);
				Response response = LOCAL.get().get(timeout, TimeUnit.MILLISECONDS);
				return response.getResult();
			}
		});
	}

	public static final class ResponseFuture<V> implements Future<V> {

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
				throw new TimeoutException();
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
	
	public static final ResponseFuture<Response> getFuture(long requestId) {
		return TEMP_FUTURE.getIfPresent(requestId);
	}

}
