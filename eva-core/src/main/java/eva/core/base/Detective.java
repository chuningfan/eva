package eva.core.base;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import eva.common.global.StatusEvent;
import eva.common.util.NetUtil;

public abstract class Detective implements Observer, Callable<Void> {

	private static final Logger LOG = Logger.getLogger("Detective");
	
	private ReentrantLock lock = new ReentrantLock();
	
	private Condition condition = lock.newCondition();
	
	private volatile boolean isConnected = false;
	
	private final Timer keeper; 
	
	public Detective() {
		keeper = new Timer();
	}
	
	@Override
	public Void call() throws Exception {
		 do {
			try {
				lock.lock();
				connect();
				Thread.sleep(30 * 1000L);
				if (isConnected) {
					condition.await();
				}
			} finally {
				lock.unlock();
			}
		 } while (true);
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		StatusEvent event = (StatusEvent) arg1;
		int status = event.getStatus();
		switch (status) {
		case 0: 
			isConnected = true;
			LOG.info("Detective connected!");
			keeper.schedule(new TimerTask() {
				@Override
				public void run() {
					InetSocketAddress address = targetAddress();
					try {
						lock.lock();
						Thread.sleep(3 * 1000L);
						boolean status = NetUtil.pingHost(address.getAddress().getHostAddress(), address.getPort());
						String targetAddress = address.getAddress().getHostAddress()+ ":" + address.getPort();
						if (status) {
							LOG.info("Heart beat keeper: Reached to " + targetAddress);
						} else {
							LOG.warning("Heart beat keeper: Cannot reach to " + targetAddress);
						}
						if (!status) {
							isConnected = false;
							condition.signal();
						}
					} catch (Exception e) {
						LOG.info(e.getMessage());
					} finally {
						lock.unlock();
					}
				}
			}, 0, 3000);
			break;
		case 1: isConnected = false;
			condition.signal();
			break;
		}
	}
	
	public abstract void connect() throws InterruptedException;
	
	public abstract InetSocketAddress targetAddress();
	
}
