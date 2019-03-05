package eva.server.core.valve.invoker.async;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Queues;

import eva.core.base.ResourceProvider;
import eva.core.base.config.ServerConfig;
import eva.core.dto.ReturnVoid;
import eva.core.exception.EvaServerException;
import eva.core.transport.Packet;
import eva.core.transport.Response;
import eva.core.valve.InvokerValve;
import eva.core.valve.Result;
import io.netty.channel.ChannelHandlerContext;

public class Processor {

	public static final int CORE_THREAD_SIZE = Runtime.getRuntime().availableProcessors();

	private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

	private static ThreadPoolExecutor TPT = null;
	
	private volatile ServerConfig config;

	static {
		int CPUCount = Runtime.getRuntime().availableProcessors();
		TPT = new ThreadPoolExecutor(CPUCount + 1, CPUCount * 2, 15, TimeUnit.SECONDS,
				Queues.newArrayBlockingQueue(500), new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						BlockingQueue<Runnable> queue = executor.getQueue();
						int time = 0;
						boolean flag = false;
						while (time++ < 3 && !flag) {
							try {
								flag = queue.offer(r, 500, TimeUnit.MILLISECONDS);
							} catch (InterruptedException e) {
								LOG.error("Retrying offer task into queue failed. " + e.getMessage());
							}
						}
						if (!flag) {
							try {
								throw new EvaServerException("TPT rejected!");
							} catch (EvaServerException e) {
								LOG.error(e.getMessage());
							}
						}
					}
				});
	}

	private static final class ProcessorHolder {
		private static final Processor INSTANCE = new Processor();
	}

	public static final Processor getInstance() {
		return ProcessorHolder.INSTANCE;
	}

	private volatile boolean started = false;

	private ReentrantLock lock = new ReentrantLock();

	public void init(ServerConfig config) {
		if (Objects.isNull(this.config)) {
			this.config = config;
		}
		if (!started) {
			try {
				if (lock.tryLock()) {
					if (!started) {
						TPT.execute(new Runner(config.getProvider()));
					}
				}
			} finally {
				lock.unlock();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static final class Runner extends InvokerValve implements Runnable {

		private final ResourceProvider provider;
		
		private Runner(ResourceProvider provider) {
			this.provider = provider;
		}
		
		@Override
		public void run() {
			ChannelHandlerContext ctx = null;
			Task task = null;
			for (;;) {
				try {
					task = Queue.getInstance().getTask();
					ctx = task.getCtx();
					Packet packet = task.getPacket();
					Class<?> interfaceClass = packet.getInterfaceClass();
					Object proxy = provider.getSource(interfaceClass);
					Response resp = new Response();
					resp.setRequestId(packet.getRequestId());
					if (Objects.nonNull(proxy)) {
						Class<?>[] types = packet.getArgTypes();
						Method method = null;
						if (Objects.nonNull(types)) {
							method = interfaceClass.getDeclaredMethod(packet.getMethodName(), types);
						} else {
							method = interfaceClass.getDeclaredMethod(packet.getMethodName());
						}
						Class<?> returnType = method.getReturnType();
						if (!"void".equalsIgnoreCase(returnType.getName())) {
							Object res = processInWrap(proxy, method, packet.getArgs());
							resp.setResult(res);
						} else {
							processInWrap(proxy, method, packet.getArgs());
							resp.setResult(ReturnVoid.getInstance());
						}
						resp.setStateCode(0);
						resp.setMessage("ok");
					} else {
						resp.setStateCode(1);
						resp.setMessage("failed");
					}
					if (ctx.channel().isActive() && ctx.channel().isOpen()) {
						ctx.writeAndFlush(resp);
					}
				} catch (Exception e) {
					LOG.error(e.getMessage());
				}
			}
		}

		@Override
		protected Result process0(Object data, Result result) throws Exception {
			return null;
		}

	}


}
