package eva.common.global;

public class RequestID {

	private long workerId;
	public static long datacenterId = 0L;
	private long sequence = 0L;
	private long twepoch = 1514736000000L;
	private long workerIdBits = 5L;
	private long datacenterIdBits = 5L;
	private long maxWorkerId = -1L ^ (-1L << workerIdBits);
	private long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
	private long sequenceBits = 12L;
	private long workerIdShift = sequenceBits;
	private long datacenterIdShift = sequenceBits + workerIdBits;
	private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
	private long sequenceMask = -1L ^ (-1L << sequenceBits); // 4095
	private long lastTimestamp = -1L;

	private static final class RequestIDHolder {
		private static final RequestID INSTANCE = new RequestID();
	}
	
	public static final RequestID getInstance() {
		return RequestIDHolder.INSTANCE;
	}
	
	private RequestID() {
		this(0L);
	}

	private RequestID(long workerId) {
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format("Invalid worker ID: %d", maxWorkerId));
		}

		if (datacenterId > maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(String.format("Invalid server ID: %d", maxDatacenterId));
		}
		this.workerId = workerId;
	}

	/**
	 * 
	 * @return
	 */
	public synchronized long get() {
		long timestamp = currentTime();
		if (timestamp < lastTimestamp) {
			throw new RuntimeException(String.format("Refused to generate ID in %d", lastTimestamp - timestamp));
		}
		if (lastTimestamp == timestamp) {
			sequence = (sequence + 1) & sequenceMask;
			if (sequence == 0) {
				timestamp = nextMillis(lastTimestamp);
			}
		} else {
			sequence = 0L;
		}
		lastTimestamp = timestamp;
		return ((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift)
				| (workerId << workerIdShift) | sequence;
	}

	/**
	 * 
	 * @param lastTimestamp
	 * @return
	 */
	protected long nextMillis(long lastTimestamp) {
		long timestamp = currentTime();
		while (timestamp <= lastTimestamp) {
			timestamp = currentTime();
		}
		return timestamp;
	}

	/**
	 * 
	 * @return
	 */
	protected long currentTime() {
		return System.currentTimeMillis();
	}

}
