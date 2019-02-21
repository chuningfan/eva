package eva.client.core.context;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import eva.common.base.BaseContext;
import eva.common.base.config.ClientConfig;
import eva.common.exception.EvaContextException;
import io.netty.util.internal.StringUtil;

public class EvaClientContext implements BaseContext {

	private static Map<String, Set<String>> REGISTRY_DATA;
	
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
			REGISTRY_DATA = Maps.newConcurrentMap();
		} else {
			throw new EvaContextException("In client configuration file, both single host and registry address are configured but expect one!");
		}
	}

}
