package eva.common.global;

public class StatusEvent {
	
	//0 successful, 1 failed
	private short status;
	
	private Object[] args;
	
	private Throwable exc;

	public short getStatus() {
		return status;
	}

	public void setStatus(short status) {
		this.status = status;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public Throwable getExc() {
		return exc;
	}

	public void setExc(Throwable exc) {
		this.exc = exc;
	}
	
	public static final StatusEvent getFailedEvent(Throwable exc) {
		StatusEvent failedEvent = new StatusEvent();
		failedEvent.setStatus((short)1);
		failedEvent.setExc(exc);
		return failedEvent;
	}

	public static final StatusEvent getCloseEvent(Object...args) {
		StatusEvent closeEvent = new StatusEvent();
		closeEvent.setStatus((short)2);
		closeEvent.setArgs(args);
		return closeEvent;
	}
	
	public static final StatusEvent getStartupEvent(Object...args) {
		StatusEvent startupEvent = new StatusEvent();
		startupEvent.setStatus((short)0);
		startupEvent.setArgs(args);
		return startupEvent;
	}
	
}
