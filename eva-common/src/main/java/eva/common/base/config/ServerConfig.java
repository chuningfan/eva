package eva.common.base.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:eva.properties")
public class ServerConfig {
	
	@Value("${eva.server.id}")
	private String serverId;
	@Value("${eva.server.port}")
	private int port;
	@Value("${eva.server.connector.size}")
	private int bossSize = 1;
	@Value("${eva.server.processor.size}")
	private int workerSize = Runtime.getRuntime().availableProcessors();
	@Value("${eva.server.enable.monitor}")
	private boolean needMonitor;
	@Value("${eva.server.enable.async}")
	private boolean asyncProcessing;
	@Value("${eva.server.async.queue.size}")
	private int asyncQueueSize = 30;

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
	
}
