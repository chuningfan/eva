package eva.server.core.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.springframework.context.ApplicationContext;

import com.google.common.collect.Queues;

import eva.core.exception.EvaServerException;
import eva.core.transport.Packet;
import eva.core.transport.Response;
import eva.server.core.context.AncientContext;
import io.netty.channel.ChannelHandlerContext;

public class Processor {

	public static final int CORE_THREAD_SIZE = Runtime.getRuntime().availableProcessors();

	private static final Logger LOG = Logger.getLogger("Server-Processor");

	private static ThreadPoolExecutor TPT = null;

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
								e.printStackTrace();
								LOG.warning("Retrying offer task into queue failed. ");
							}
						}
						if (!flag) {
							try {
								throw new EvaServerException("TPT rejected!");
							} catch (EvaServerException e) {
								e.printStackTrace();
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

	public void init() {
		if (!started) {
			try {
				if (lock.tryLock()) {
					if (!started) {
						TPT.execute(new Runner());
					}
				}
			} finally {
				lock.unlock();
			}
		}
	}

	private static final class Runner implements Runnable {

		private static final ApplicationContext CONTEXT = AncientContext.CONTEXT;

		@Override
		public void run() {
			for (;;) {
				try {
					Task task = Queue.getInstance().getTask();
					Packet packet = task.getPacket();
					Class<?> interfaceClass = packet.getInterfaceClass();
					Object proxy = CONTEXT.getBean(interfaceClass);
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
							Object res = method.invoke(proxy, packet.getArgs());
							resp.setResult(res);
						}
						resp.setStateCode(0);
						resp.setMessage("ok");
					} else {
						resp.setStateCode(1);
						resp.setMessage("failed");
					}
					ChannelHandlerContext ctx = task.getCtx();
					if (ctx.channel().isActive() && ctx.channel().isOpen()) {
						ctx.writeAndFlush(resp);
					}
				} catch (InterruptedException | NoSuchMethodException | SecurityException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
