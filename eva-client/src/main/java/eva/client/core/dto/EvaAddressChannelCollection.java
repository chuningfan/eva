package eva.client.core.dto;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class EvaAddressChannelCollection<T> extends LinkedList<T> {

	private static final long serialVersionUID = 1L;
	private ReentrantLock offerLock = new ReentrantLock();
	private ReentrantLock pollLock = new ReentrantLock();
	private int coreSize;
	private int maxSize;
	private volatile int currentSize;

	public EvaAddressChannelCollection(int coreSize, int maxSize, String addr) {
		this.coreSize = coreSize;
		this.maxSize = maxSize;
	}

	@Override
	public boolean offer(T arg0) {
		try {
			offerLock.lock();
			if (size() >= maxSize) {
				return false;
			}
			boolean res = super.offer(arg0);
			if (res) {
				currentSize++;
			}
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
			T res = super.poll();
			if (Objects.nonNull(res)) {
				currentSize--;
			}
			return res;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return poll();
		} finally {
			pollLock.unlock();
		}
	}

	public T WatcherPoll() {
		if (pollLock.tryLock()) {
			T res = super.poll();
			if (Objects.nonNull(res)) {
				currentSize--;
			}
			pollLock.unlock();
			return res;
		}
		return null;
	}

	public int getCurrentSize() {
		return currentSize;
	}

	public void setCurrentSize(int currentSize) {
		this.currentSize = currentSize;
	}
	
}
