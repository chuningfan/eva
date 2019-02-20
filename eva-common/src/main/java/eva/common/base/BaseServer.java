package eva.common.base;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Observable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.common.base.config.ServerConfig;
import eva.common.dto.Event;
import eva.common.dto.Status;
import io.netty.util.concurrent.DefaultThreadFactory;

public abstract class BaseServer extends Observable implements Future<Boolean> {
	
	private static final Logger LOG = LoggerFactory.getLogger(BaseServer.class);
	
	private final ExecutorService daemon;
	
	protected final AtomicBoolean serverStatus = new AtomicBoolean();
	
	private Status status = Status.STOPPED;
	
	protected final ServerConfig config;
	
	private Future<?> future;
	
	protected final ReentrantLock lock = new ReentrantLock();
	
	public BaseServer(ServerConfig config) {
		status = Status.INITIALIZING;
		daemon = Executors.newSingleThreadExecutor(new DefaultThreadFactory(getClass()) {
			@Override
			public Thread newThread(Runnable r) {
				final Thread thread = Executors.defaultThreadFactory().newThread(r);
		        thread.setName(getClass().getSimpleName() + "_daemon_" + config.getServerId());
		        thread.setDaemon(true);
		        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						e.printStackTrace();
						LOG.error(e.getMessage());
						t.interrupt();
					}
		        };
		        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
		        return thread;
			}
		});
		this.config = config;
	}

	public void start() {
		if (serverStatus.compareAndSet(false, true)) {
			future = daemon.submit(() -> {
				init(config);
			});
			status = Status.STARTED;
		}
	}
	
	public void stop() {
		cancel(true);
	}
	
	public void stopGracefully() {
		cancel(false);
	}
	
	protected abstract void init(ServerConfig config);
	
	protected void notifyObservers(Event arg) {
		setChanged();
		super.notifyObservers(arg);
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (serverStatus.compareAndSet(true, false)) {
			boolean flag = future.cancel(mayInterruptIfRunning);
			status = Status.STOPPED;
			return flag;
		}
		return false;
	}
	
	@Override
	public boolean isDone() {
		return status == Status.STARTED;
	}
	
	@Override
	public boolean isCancelled() {
		return status == Status.STOPPED;
	}
	
	@Override
	public Boolean get() throws InterruptedException, ExecutionException {
		return null;
	}
	
	@Override
	public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}
	
	protected abstract <T> T getDecoder();
	
	protected abstract <T> T getEncoder();
	
}
