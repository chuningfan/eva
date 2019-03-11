package eva.common.monitor;

import java.util.Date;

public class EvaDataForMonitor {
	
	private Date time;
	
	private int threadCount;
	
	private int peakThreadCount;
	
	private long usedHeap;
	
	private long usedNonHeap;
	
	private long maxHeap;
	
	private Date startTime;
	
	private Date upTime;

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public int getThreadCount() {
		return threadCount;
	}

	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public int getPeakThreadCount() {
		return peakThreadCount;
	}

	public void setPeakThreadCount(int peakThreadCount) {
		this.peakThreadCount = peakThreadCount;
	}

	public long getUsedHeap() {
		return usedHeap;
	}

	public void setUsedHeap(long usedHeap) {
		this.usedHeap = usedHeap;
	}

	public long getUsedNonHeap() {
		return usedNonHeap;
	}

	public void setUsedNonHeap(long usedNonHeap) {
		this.usedNonHeap = usedNonHeap;
	}

	public long getMaxHeap() {
		return maxHeap;
	}

	public void setMaxHeap(long maxHeap) {
		this.maxHeap = maxHeap;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getUpTime() {
		return upTime;
	}

	public void setUpTime(Date upTime) {
		this.upTime = upTime;
	}
	
}
