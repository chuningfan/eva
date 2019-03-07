package eva.client.core.context;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.common.global.RequestID;
import eva.common.global.StatusEvent;
import eva.common.util.NetUtil;
import eva.core.base.BaseContext;
import eva.core.base.Detective;
import eva.core.base.config.ClientConfig;
import eva.core.exception.EvaContextException;
import io.netty.util.internal.StringUtil;

public class EvaClientContext extends BaseContext<ClientConfig> {

	private static final Logger LOG = LoggerFactory.getLogger(EvaClientContext.class);

	private final ClientProvider clientProvider;

	private final ExecutorService daemon;

	public EvaClientContext(ClientConfig config)
			throws EvaContextException, InterruptedException, KeeperException, IOException {
		super(config);
		clientProvider = ClientProvider.get();
		daemon = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable arg0) {
				final Thread th = Executors.defaultThreadFactory().newThread(arg0);
				th.setDaemon(true);
				th.setContextClassLoader(getClass().getClassLoader());
				return th;
			}
		});
		init();
	}

	@Override
	protected void init() throws EvaContextException, InterruptedException, KeeperException, IOException {
		RequestID.datacenterId = parameter.getClientId();
		if (!StringUtil.isNullOrEmpty(parameter.getSingleHostAddress())
				&& StringUtil.isNullOrEmpty(parameter.getRegistryAddress())) {
			clientProvider.setSingleHost(true);
			clientProvider.setServerAddress(parameter.getSingleHostAddress());
		} else if (StringUtil.isNullOrEmpty(parameter.getSingleHostAddress())
				&& !StringUtil.isNullOrEmpty(parameter.getRegistryAddress())) {
			clientProvider.setSingleHost(false);
			clientProvider.setServerAddress(parameter.getRegistryAddress());
		} else {
			throw new EvaContextException(
					"In client configuration file, both single host and registry address are configured but expect one!");
		}
		clientProvider.setConfig(parameter);
			Detective singleHostConnectionDetective = new Detective() {
				@Override
				public void connect() throws InterruptedException, KeeperException, IOException {
					try {
						lock.lock();
						StatusEvent event = StatusEvent.getStartupEvent();
						if (clientProvider.prepare()) {
							event.setStatus((short) 0);
							LOG.info("Prepared channels.");
						} else {
							LOG.error("Cannot prepare channels!");
							event.setStatus((short) 1);
						}
						setChanged();
						notifyObservers(event);
					} finally {
						lock.unlock();
					}
				}

				@Override
				public InetSocketAddress targetAddress() {
					return NetUtil.getAddress(clientProvider.getServerAddress());
				}
			};
			addObserver(singleHostConnectionDetective);
			daemon.submit(singleHostConnectionDetective);
			clientProvider.prepare();

	}

}
