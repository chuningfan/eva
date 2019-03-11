package eva.client.core.context;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import eva.common.util.NetUtil;

public abstract class EvaAddressChannelCollection<T> extends LinkedList<T> {

	private static final long serialVersionUID = 1L;
	private ReentrantLock offerLock = new ReentrantLock();
	private ReentrantLock pollLock = new ReentrantLock();
	private int coreSize;
	private int maxSize;
	private String addr;
	private volatile int currentSize;

	public EvaAddressChannelCollection(int coreSize, int maxSize, String addr) {
		this.coreSize = coreSize;
		this.maxSize = maxSize;
		this.addr = addr;
	}

	@Override
	public boolean offer(T arg0) {
		if (Objects.isNull(arg0)) {
			return false;
		} 
		try {
			offerLock.lock();
			if (currentSize >= maxSize) {
				return false;
			}
			boolean res = super.offer(arg0);
			if (res) {
				currentSize ++;
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
			if (currentSize < 1 && currentSize < maxSize) {
				T ret = (T) createSource(NetUtil.getAddress(addr));
				if (Objects.nonNull(ret)) {
					currentSize ++;
					return ret;
				}
			}
			T res = super.poll();
			if (Objects.nonNull(res)) {
				currentSize --;
			}
			return res;
		} finally {
			pollLock.unlock();
		}
	}

	public T watcherPoll() {
		if (pollLock.tryLock()) {
			T res = super.poll();
			if (Objects.nonNull(res)) {
				currentSize --;
			}
			pollLock.unlock();
			return res;
		}
		return null;
	}
	
	public abstract T createSource(Object...args);
	
}
