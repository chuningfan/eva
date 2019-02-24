package eva.core.base;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.common.util.NetUtil;
import eva.core.dto.StatusEvent;

public abstract class Detective implements Observer, Callable<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(Detective.class);
	
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
				Thread.sleep(1000L);
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
						Thread.sleep(15000L);
						boolean status = NetUtil.pingHost(address.getAddress().getHostAddress(), address.getPort());
						String targetAddress = address.getAddress().getHostAddress()+ ":" + address.getPort();
						if (status) {
							LOG.trace("Heart beat keeper: Reached to " + targetAddress);
						} else {
							LOG.warn("Heart beat keeper: Cannot reach to " + targetAddress);
						}
						if (!status) {
							isConnected = false;
							condition.signal();
						}
					} catch (Exception e) {
						e.printStackTrace();
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
	
	public abstract void connect();
	
	public abstract InetSocketAddress targetAddress();
	
	
	
}
