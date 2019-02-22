package eva.client.core.context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Maps;

import eva.client.core.dto.ClientWrap;
import eva.common.global.RequestID;
import eva.core.annotation.EvaCall;
import eva.core.base.config.ClientConfig;
import eva.core.transport.Packet;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class RPCClient {

	private static final ThreadLocal<ResponseFuture> LOCAL = new ThreadLocal<ResponseFuture>() {
		@Override
		protected ResponseFuture initialValue() {
			return new ResponseFuture();
		}
	};

	private static volatile Map<Long, ResponseFuture> TEMP_FUTURE = Maps.newConcurrentMap();

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
				Packet p = new Packet();
				p.setArgs(methodArgs);
				p.setArgTypes(argTypes);
				p.setInterfaceClass(interfaceClass);
				p.setMethodName(methodName);
				p.setRequestId(RequestID.get());
				@SuppressWarnings("static-access")
				ClientWrap wrap = EvaClientContext.CONTEXT.getBean(EvaClientContext.class).getClientProvider().get()
						.getSource(interfaceClass);
				ClientConfig config = EvaClientContext.CONTEXT.getBean(EvaClientContext.class).getConfig();
				long timeout = config.getGlobalTimoutMilliSec();
				EvaCall call = interfaceClass.getAnnotation(EvaCall.class);
				if (Objects.nonNull(call)) {
					int timeoutVal = call.timeout();
					timeout = call.timeUnit().toMillis(timeoutVal);
				}
				wrap.getChannel().writeAndFlush(p);
				ResponseFuture f = LOCAL.get();
				TEMP_FUTURE.put(p.getRequestId(), f);
				Object result = LOCAL.get().get(timeout, TimeUnit.MILLISECONDS);
				return null;
			}
		});
	}

	private static final class ResponseFuture implements Future<Object> {

		private long timeoutMilliSec;

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public Object get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}

		@Override
		public boolean isSuccess() {
			return false;
		}

		@Override
		public boolean isCancellable() {
			return false;
		}

		@Override
		public Throwable cause() {
			return null;
		}

		@Override
		public Future<Object> addListener(GenericFutureListener<? extends Future<? super Object>> listener) {
			return null;
		}

		@Override
		public Future<Object> addListeners(GenericFutureListener<? extends Future<? super Object>>... listeners) {
			return null;
		}

		@Override
		public Future<Object> removeListener(GenericFutureListener<? extends Future<? super Object>> listener) {
			return null;
		}

		@Override
		public Future<Object> removeListeners(GenericFutureListener<? extends Future<? super Object>>... listeners) {
			return null;
		}

		@Override
		public Future<Object> sync() throws InterruptedException {
			return null;
		}

		@Override
		public Future<Object> syncUninterruptibly() {
			return null;
		}

		@Override
		public Future<Object> await() throws InterruptedException {
			return null;
		}

		@Override
		public Future<Object> awaitUninterruptibly() {
			return null;
		}

		@Override
		public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
			return false;
		}

		@Override
		public boolean await(long timeoutMillis) throws InterruptedException {
			return false;
		}

		@Override
		public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
			return false;
		}

		@Override
		public boolean awaitUninterruptibly(long timeoutMillis) {
			return false;
		}

		@Override
		public Object getNow() {
			return null;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		public long getTimeoutMilliSec() {
			return timeoutMilliSec;
		}

		public void setTimeoutMilliSec(long timeoutMilliSec) {
			this.timeoutMilliSec = timeoutMilliSec;
		}

	}

}
