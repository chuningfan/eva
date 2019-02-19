package eva.common.dto;

public class Event {
	
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
	
}
