package eva.server.core.monitor;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.core.base.BaseServer;
import eva.core.base.config.ServerConfig;

public class MonitorDataServer extends BaseServer {

	private static final Logger LOG = LoggerFactory.getLogger(MonitorDataServer.class);
	
	public MonitorDataServer(ServerConfig config) {
		super(config);
	}

	@Override
	protected void init(ServerConfig config) {
		try {
			MonitorHelper.startAndProcess(config);
		} catch (IOException e) {
			e.printStackTrace();
			LOG.warn("cannot start up monitor support server, please check if you need this support.");
		}
	}

	@Override
	protected <T> T getDecoder() {
		return null;
	}

	@Override
	protected <T> T getEncoder() {
		return null;
	}

}
