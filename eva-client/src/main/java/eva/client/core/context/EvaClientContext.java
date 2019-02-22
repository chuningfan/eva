package eva.client.core.context;

import java.util.Map;
import java.util.Set;

import eva.balance.strategies.BalanceStrategyFactory;
import eva.client.core.dto.ClientWrap;
import eva.common.base.BaseContext;
import eva.common.base.config.ClientConfig;
import eva.common.exception.EvaClientException;
import eva.common.exception.EvaContextException;
import eva.common.registry.Registry;
import eva.common.transport.Packet;
import io.netty.util.internal.StringUtil;
import test.TestInterface;

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
		clientProvider.prepare();
	}

	public ClientProvider getClientProvider() {
		return clientProvider;
	}

	public static void main(String[] args) throws EvaContextException, EvaClientException {
		ClientConfig config = new ClientConfig();
		config.setMaxSizePerProvider(3);
		config.setSingleHostAddress("127.0.0.1:8763");
		EvaClientContext ctx = new EvaClientContext(config);
		ClientWrap ch = ctx.getClientProvider().getSource(TestInterface.class);
		Packet p = new Packet();
		p.setRequestId(111L);
		p.setArgTypes(null);
		p.setInterfaceClass(TestInterface.class);
		p.setMethodName("test");
		ch.getChannel().writeAndFlush(p);
	}
	
}
