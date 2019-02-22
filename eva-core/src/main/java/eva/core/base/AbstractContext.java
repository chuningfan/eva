package eva.core.base;

import java.util.Objects;
import java.util.Observable;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractContext extends Observable {

	protected static ClassLoader LOADER = null;

	protected final ReentrantLock lock = new ReentrantLock();
	
	{
		if (Objects.isNull(LOADER)) {
			try {
				if (lock.tryLock()) {
					if (Objects.isNull(LOADER)) {
						LOADER = Thread.currentThread().getContextClassLoader();
					}
					if (Objects.isNull(LOADER)) {
						LOADER = AbstractContext.class.getClassLoader();
					}
				}
			} finally {
				lock.unlock();
			}
		}
	}
	
}
