package eva.client.core.context;

import java.util.Map;
import java.util.Set;

import eva.common.base.BaseContext;
import eva.common.base.config.ClientConfig;
import eva.common.exception.EvaClientException;
import eva.common.exception.EvaContextException;
import eva.common.registry.Registry;
import eva.common.transport.Packet;
import io.netty.channel.Channel;
import io.netty.util.internal.StringUtil;

class EvaClientContext implements BaseContext {

	// key: provider name, value addresses
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
			REGISTRY_DATA = Registry.get().getAllNodes();
		} else {
			throw new EvaContextException("In client configuration file, both single host and registry address are configured but expect one!");
		}
		clientProvider.setMaxSizePerProvider(config.getMaxSizePerProvider());
	}

	public ClientProvider getClientProvider() {
		return clientProvider;
	}

	public static void main(String[] args) throws EvaContextException, EvaClientException {
		ClientConfig config = new ClientConfig();
		config.setMaxSizePerProvider(3);
		config.setSingleHostAddress("127.0.0.1:8763");
		EvaClientContext ctx = new EvaClientContext(config);
		Channel ch = ctx.getClientProvider().create(null);
		Packet p = new Packet();
		ch.writeAndFlush(p);
	}
	
}
