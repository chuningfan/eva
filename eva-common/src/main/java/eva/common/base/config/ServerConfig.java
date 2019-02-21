package eva.common.base.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:eva.properties")
public class ServerConfig {
	
	@Value("${eva.server.id}")
	private String serverId = UUID.randomUUID().toString();
	@Value("${eva.server.port}")
	private int port;
	@Value("${eva.server.connector.size}")
	private int bossSize = 1;
	@Value("${eva.server.processor.size}")
	private int workerSize = Runtime.getRuntime().availableProcessors();
	@Value("${eva.server.enable.monitor}")
	private boolean needMonitor;
	@Value("${eva.server.enable.asyncProcessing}")
	private boolean asyncProcessing;
	@Value("${eva.server.async.queue.size}")
	private int asyncQueueSize = 30;
	@Value("${eva.registry.address}")
	private String registryAddress;

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
	
}
