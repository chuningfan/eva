package eva.client.core.context;

import java.util.Map;
import java.util.Set;

import eva.balance.strategies.BalanceStrategyFactory;
import eva.common.global.RequestID;
import eva.core.base.BaseContext;
import eva.core.base.config.ClientConfig;
import eva.core.exception.EvaContextException;
import eva.core.registry.Registry;
import io.netty.util.internal.StringUtil;

class EvaClientContext implements BaseContext {

	// key: interface name, value addresses
	static Map<String, Set<String>> REGISTRY_DATA;
	
	private final ClientConfig config;
	
	private final ClientProvider clientProvider = ClientProvider.get();
	
	public EvaClientContext(ClientConfig config) throws EvaContextException {
		this.config = config;
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
		clientProvider.setMaxSizePerHost(config.getMaxSizePerProvider());
		clientProvider.setGlobalTimeoutMillSec(config.getGlobalTimoutMilliSec());
		clientProvider.prepare();
	}

	public ClientProvider getClientProvider() {
		return clientProvider;
	}

	ClientConfig getConfig() {
		return config;
	}
	
}
