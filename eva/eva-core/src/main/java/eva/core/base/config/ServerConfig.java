package eva.core.base.config;

import java.util.UUID;

import eva.core.base.ResourceProvider;

public class ServerConfig {
	
	private String serverId = UUID.randomUUID().toString();
	private int port = 8763;
	private int bossSize = 1;
	private int workerSize = Runtime.getRuntime().availableProcessors();
	private boolean needMonitor;
	private boolean asyncProcessing;
	private int asyncQueueSize = 30;
	private String registryAddress;
	private int serverTimeoutSec = -1;
	private volatile ResourceProvider provider;
	private boolean jmxSupport;
	private boolean monitorSupport;
	private int monitorSupportPort = 8220;
	private String daemonName;
	
	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getBossSize() {
		return bossSize;
	}

	public void setBossSize(int bossSize) {
		this.bossSize = bossSize;
	}

	public int getWorkerSize() {
		return workerSize;
	}

	public void setWorkerSize(int workerSize) {
		this.workerSize = workerSize;
	}

	public boolean isNeedMonitor() {
		return needMonitor;
	}

	public void setNeedMonitor(boolean needMonitor) {
		this.needMonitor = needMonitor;
	}

	public boolean isAsyncProcessing() {
		return asyncProcessing;
	}

	public void setAsyncProcessing(boolean asyncProcessing) {
		this.asyncProcessing = asyncProcessing;
	}

	public int getAsyncQueueSize() {
		return asyncQueueSize;
	}

	public void setAsyncQueueSize(int asyncQueueSize) {
		this.asyncQueueSize = asyncQueueSize;
	}

	public String getRegistryAddress() {
		return registryAddress;
	}

	public void setRegistryAddress(String registryAddress) {
		this.registryAddress = registryAddress;
	}

	public int getServerTimeoutSec() {
		return serverTimeoutSec;
	}

	public void setServerTimeoutSec(int serverTimeoutSec) {
		this.serverTimeoutSec = serverTimeoutSec;
	}

	public ResourceProvider getProvider() {
		return provider;
	}

	public void setProvider(ResourceProvider provider) {
		this.provider = provider;
	}

	public boolean isJmxSupport() {
		return jmxSupport;
	}

	public void setJmxSupport(boolean jmxSupport) {
		this.jmxSupport = jmxSupport;
	}

	public boolean isMonitorSupport() {
		return monitorSupport;
	}

	public void setMonitorSupport(boolean monitorSupport) {
		this.monitorSupport = monitorSupport;
	}

	public int getMonitorSupportPort() {
		return monitorSupportPort;
	}

	public void setMonitorSupportPort(int monitorSupportPort) {
		this.monitorSupportPort = monitorSupportPort;
	}

	public String getDaemonName() {
		return daemonName;
	}

	public void setDaemonName(String daemonName) {
		this.daemonName = daemonName;
	}
	
}
