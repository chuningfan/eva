package eva.core.base;

import java.io.IOException;
import java.util.Observable;

import org.apache.zookeeper.KeeperException;

import eva.common.global.ProviderMetadata;
import eva.common.registry.Registry;
import eva.core.base.config.ServerConfig;
import eva.core.exception.EvaContextException;

public abstract class BaseContext<P> extends Observable implements RegistryProvider<P> {

	protected P parameter;
	
	protected BaseContext(P parameter) {
		this.parameter = parameter;
	}
	
	protected abstract void init() throws EvaContextException, InterruptedException, IOException, KeeperException;

	@Override
	public Iterable<String> getAddressesByServiceName(String interfaceClassName) {
		return Registry.REGISTRY_DATA.get(interfaceClassName);
	}

	@Override
	public void doRegister(P parameter, ProviderMetadata metadata) throws IOException, KeeperException, InterruptedException {
		Registry reg = Registry.get();
		if (parameter instanceof ServerConfig) {
			ServerConfig config = (ServerConfig) parameter;
			reg.registerServerToRegistry(config.getRegistryAddress(), metadata);
			setChanged();
			notifyObservers();
		}
	}
	
}
