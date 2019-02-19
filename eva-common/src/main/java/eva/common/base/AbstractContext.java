package eva.common.base;

import java.util.Objects;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractContext<Config> implements Observer {

	protected static ClassLoader LOADER = null;

	protected final ReentrantLock lock = new ReentrantLock();
	
	protected Config config;
	
	public AbstractContext(Config config) throws Throwable{
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
		this.config = config;
		init();
	}
	
	public abstract void init()throws Throwable;

}
