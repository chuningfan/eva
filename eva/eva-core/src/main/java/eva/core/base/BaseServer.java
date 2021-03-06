package eva.core.base;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.common.global.StatusEvent;
import eva.core.base.config.ServerConfig;
import eva.core.dto.Status;
import eva.core.listener.StatusListener;
import io.netty.util.concurrent.DefaultThreadFactory;

public abstract class BaseServer extends Observable implements Future<Boolean> {

	private static final Logger LOG = LoggerFactory.getLogger(BaseServer.class);

	private final ExecutorService daemon;

	protected final AtomicBoolean serverStatus = new AtomicBoolean(false);

	private volatile Status status = Status.STOPPED;

	protected final ServerConfig config;

	private Future<?> future;

	protected String host;

	public BaseServer(ServerConfig config) {
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			LOG.error(e1.getMessage());
		}
		status = Status.INITIALIZING;
		daemon = Executors.newSingleThreadExecutor(new DefaultThreadFactory(getClass()) {
			@Override
			public Thread newThread(Runnable r) {
				final Thread thread = Executors.defaultThreadFactory().newThread(r);
				String daemonName = (config.getDaemonName() == null || config.getDaemonName().trim().length() < 1) ? "Server-Daemon-" : config.getDaemonName();
				thread.setName(daemonName + config.getServerId());
				thread.setDaemon(true);
				final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						notifyObservers(StatusEvent.getFailedEvent(e));
						LOG.info(e.getMessage());
					}
				};
				thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
				return thread;
			}
		});
		this.config = config;
		StatusListener serverStartupListener = new StatusListener() {
			@Override
			public void onSuccess(Observable source, StatusEvent event) {
				if (serverStatus.compareAndSet(false, true)) {
					status = Status.STARTED;
				}
			}

			@Override
			public void onFailure(Observable source, StatusEvent event) {
				if (serverStatus.compareAndSet(true, false)) {
					status = Status.STOPPED;
					daemon.shutdownNow();
					LOG.info("Eva startup failed: " + event.getExc().getMessage());
				}
			}

			@Override
			public void onClose(Observable source, StatusEvent event) {
				if (serverStatus.compareAndSet(true, false)) {
					status = Status.STOPPED;
					daemon.shutdown();
					LOG.info("Eva shutted down");
				}
			}
		};
		addObserver(serverStartupListener);
	}

	public void start() {
		future = daemon.submit(() -> {
			init(config);
		});
	}

	public void stop() {
		cancel(true);
	}

	public void stopGracefully() {
		cancel(false);
	}

	protected abstract void init(ServerConfig config);

	protected void notifyObservers(StatusEvent arg) {
		setChanged();
		super.notifyObservers(arg);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		boolean flag = future.cancel(mayInterruptIfRunning);
		return flag;
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
		return serverStatus.get();
	}

	@Override
	public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return serverStatus.get();
	}

	public Status getServerStatus() {
		return status;
	}

	protected abstract <T> T getDecoder();

	protected abstract <T> T getEncoder();

}
