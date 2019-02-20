package eva.server.core.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.ApplicationContext;

import com.google.common.collect.Queues;

import eva.common.dto.RequestStatus;
import eva.common.dto.RequestStatus.Status;
import eva.common.exception.EvaServerException;
import eva.common.transport.Packet;
import eva.common.util.PacketUtil;
import eva.server.core.context.AncientContext;
import io.netty.channel.ChannelHandlerContext;

public class Processor {

	public static final int CORE_THREAD_SIZE = Runtime.getRuntime().availableProcessors();

	private static ThreadPoolExecutor TPT = null;

	static {
		TPT = new ThreadPoolExecutor(CORE_THREAD_SIZE, (CORE_THREAD_SIZE * 2), 3, TimeUnit.SECONDS,
				Queues.newArrayBlockingQueue(500), new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						BlockingQueue<Runnable> queue = executor.getQueue();
						int time = 0;
						boolean flag = false;
						while (time++ < 3 && !flag) {
							flag = queue.add(r);
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
			try {
				Task task = Queue.getInstance().getTask();
				Packet packet = task.getPacket();
				Class<?> interfaceClass = packet.getBody().getInterfaceClass();
				Object proxy = CONTEXT.getBean(interfaceClass);
				Packet resp = new Packet();
				resp.setRequestId(packet.getRequestId());
				if (Objects.nonNull(proxy)) {
					Class<?>[] types = PacketUtil.getTypes(packet.getBody().getArgs());
					Method method = null;
					if (Objects.nonNull(types)) {
						method = interfaceClass.getDeclaredMethod(packet.getBody().getMethodName(), types);
					} else {
						method = interfaceClass.getDeclaredMethod(packet.getBody().getMethodName());
					}
					Class<?> returnType = method.getReturnType();
					if (!"void".equalsIgnoreCase(returnType.getName())) {
						Object res = method.invoke(proxy, packet.getBody().getArgs());
						resp.getBody().setResponse(res);
					}
					resp.getBody().setReturnType(returnType);
					resp.getBody().setStatus(new RequestStatus.Builder(Status.SUCCESSFUL, "ok").build());
				} else {
					resp.getBody()
							.setStatus(new RequestStatus.Builder(Status.FAILED,
									"Cannot find proxy instance in context for interface [" + interfaceClass
											+ "]; request ID is " + packet.getRequestId()).build());
				}
				PacketUtil.setBodySize(resp);
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
