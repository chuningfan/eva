package eva.core.base;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.common.global.StatusEvent;

public abstract class Detective implements Observer, Callable<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(Detective.class);
	
	protected ReentrantLock lock = new ReentrantLock();
	
	private Condition condition = lock.newCondition();
	
	private volatile boolean isConnected = false;
	
	private final Timer keeper; 
	
	public Detective() {
		keeper = new Timer();
	}
	
	@Override
	public Void call() throws Exception {
		 do {
			 Thread.sleep(10 * 1000L);
			try {
				lock.lock();
				connect();
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
				private Socket connect = new Socket();
				private volatile InetSocketAddress endpointSocketAddr;
				@Override
				public void run() {
					try {
						lock.lock();
						Thread.sleep(3 * 1000L);
						if (Objects.isNull(endpointSocketAddr)) {
							endpointSocketAddr = targetAddress();
						}
						connect.connect(endpointSocketAddr,3000);
						if (connect.isConnected()) {
//							LOG.info("Heart beat keeper: Reached to " + targetAddress);
						} else {
							LOG.error("Heart beat keeper: Cannot reach to " + endpointSocketAddr.getAddress() + ":" + endpointSocketAddr.getPort());
						}
						if (!connect.isConnected()) {
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
	
	public abstract void connect() throws InterruptedException, KeeperException, IOException;
	
	public abstract InetSocketAddress targetAddress();
	
}
