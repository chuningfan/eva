package eva.common.dto;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import eva.common.transport.Packet;

public class ResponseFuture implements Future<Packet> {

	private ReentrantLock lock = new ReentrantLock();
	
	private Condition condition = lock.newCondition();
	
	@Override
	public boolean cancel(boolean arg0) {
		return false;
	}

	@Override
	public Packet get() throws InterruptedException, ExecutionException {
		return null;
	}

	@Override
	public Packet get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return false;
	}

}
