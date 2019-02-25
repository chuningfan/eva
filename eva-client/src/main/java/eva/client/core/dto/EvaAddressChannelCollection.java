package eva.client.core.dto;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public class EvaAddressChannelCollection<T> extends LinkedList<T> {
	
	private static final long serialVersionUID = 1L;
	private ReentrantLock offerLock = new ReentrantLock();
	private ReentrantLock pollLock = new ReentrantLock();
	private int coreSize;
	
	public EvaAddressChannelCollection(int coreSize) {
		this.coreSize = coreSize;
	}

	@Override
	public boolean offer(T arg0) {
		try {
			offerLock.lock();
			if (size() >= coreSize) {
				return false;
			}
			boolean res = super.offer(arg0);
			return res;
		} finally {
			offerLock.unlock();
		}
	}

	@Override
	public T poll() {
		try {
			pollLock.lock();
			while (size() < 1) {
				Thread.sleep(100L);
			}
			return super.poll();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return poll();
		} finally {
			pollLock.unlock();
		}
	}
	
	
	
}
