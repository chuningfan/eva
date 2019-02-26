package eva.client.core.context;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.balance.strategies.BalanceStrategyFactory;
import eva.common.global.RequestID;
import eva.common.util.NetUtil;
import eva.core.base.AbstractContext;
import eva.core.base.BaseContext;
import eva.core.base.Detective;
import eva.core.base.config.ClientConfig;
import eva.core.dto.StatusEvent;
import eva.core.exception.EvaContextException;
import eva.core.registry.Registry;
import io.netty.util.internal.StringUtil;

public class EvaClientContext extends AbstractContext implements BaseContext {

	private static final Logger LOG = LoggerFactory.getLogger(EvaClientContext.class);
	
	// key: interface name, value addresses
	static Map<String, Set<String>> REGISTRY_DATA;
	
	private final ClientConfig config;
	
	private final ClientProvider clientProvider = ClientProvider.get();
	
	private final ExecutorService daemon;
	
	public EvaClientContext(ClientConfig config) throws EvaContextException {
		this.config = config;
		daemon = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable arg0) {
				final Thread th = Executors.defaultThreadFactory().newThread(arg0);
				th.setDaemon(true);
				th.setContextClassLoader(LOADER);
				return th;
			}
		});
		init();
	}
	
	@Override
	public <T> T getBean(Class<T> beanClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeBean(Class<?> beanClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void init() throws EvaContextException {
		RequestID.datacenterId = config.getClientId();
		if (!StringUtil.isNullOrEmpty(config.getSingleHostAddress()) && StringUtil.isNullOrEmpty(config.getRegistryAddress())) {
			clientProvider.setSingleHost(true);
			clientProvider.setServerAddress(config.getSingleHostAddress());
		} else if (StringUtil.isNullOrEmpty(config.getSingleHostAddress()) && !StringUtil.isNullOrEmpty(config.getRegistryAddress())) {
			clientProvider.setSingleHost(false);
			clientProvider.setServerAddress(config.getRegistryAddress());
			clientProvider.setBalanceStrategy(BalanceStrategyFactory.getStrategy(config));
			REGISTRY_DATA = Registry.get().getAllNodes();
		} else {
			throw new EvaContextException("In client configuration file, both single host and registry address are configured but expect one!");
		}
		clientProvider.setGlobalTimeoutMillSec(config.getGlobalTimoutMilliSec());
		clientProvider.setCoreSizePerHost(config.getCoreSizePerHost());
		clientProvider.setMaxSizePerHost(config.getMaxSizePerHost());
		// if the client is connected to a single host
		if (Objects.nonNull(config.getSingleHostAddress()) && Objects.isNull(config.getRegistryAddress())) {
			// start a daemon for re-connect the host's netty server if the connection is disconnected.
			Detective singleHostConnectionDetective = new Detective() {
				@Override
				public void connect() {
					StatusEvent event = StatusEvent.getStartupEvent();
					try {
						clientProvider.prepare();
					} catch (Exception e) {
						LOG.error("Cannot prepare channels, because of " + e.getMessage());
						event.setStatus((short)1);
					}
					setChanged();
					notifyObservers(event);
				}
				@Override
				public InetSocketAddress targetAddress() {
					return NetUtil.getAddress(clientProvider.getServerAddress());
				}
			};
			addObserver(singleHostConnectionDetective);
			daemon.submit(singleHostConnectionDetective);
		} else {
			clientProvider.prepare();
		}
	}

	public ClientProvider getClientProvider() {
		return clientProvider;
	}

	ClientConfig getConfig() {
		return config;
	}
	
}
