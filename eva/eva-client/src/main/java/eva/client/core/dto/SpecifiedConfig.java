package eva.client.core.dto;

import java.util.concurrent.TimeUnit;

public class SpecifiedConfig {
	
	private int timeout = 3;
	
	private TimeUnit timeoutUnit = TimeUnit.SECONDS;
	
	private Object fallback;

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public TimeUnit getTimeoutUnit() {
		return timeoutUnit;
	}

	public void setTimeoutUnit(TimeUnit timeoutUnit) {
		this.timeoutUnit = timeoutUnit;
	}

	public Object getFallback() {
		return fallback;
	}

	public void setFallback(Object fallback) {
		this.fallback = fallback;
	}
	
}
