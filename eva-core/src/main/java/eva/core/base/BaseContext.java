package eva.core.base;

import java.io.IOException;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.apache.zookeeper.KeeperException;

import com.google.common.collect.Maps;

import eva.common.global.ProviderMetadata;
import eva.common.global.RegistryChgData;
import eva.common.registry.Registry;
import eva.core.base.config.ServerConfig;
import eva.core.exception.EvaContextException;

public abstract class BaseContext<P> extends Observable implements Observer, RegistryProvider<P> {

	protected P parameter;
	
	protected Map<String, Set<String>> serviceAndAddresses = Maps.newConcurrentMap();
	
	protected BaseContext(P parameter) {
		this.parameter = parameter;
		// add current object as an observer to registry
		Registry.get().addObserver(this);
	}
	
	protected abstract void init() throws EvaContextException, InterruptedException;

	// Receive event for [registry-data] change
	@Override
	public void update(Observable source, Object event) {
		if (event instanceof RegistryChgData) {
			RegistryChgData rData = (RegistryChgData) event;
			String serviceName = rData.getInterfaceClassName();
			switch(rData.getAction()) {
			case ADD_SERVICE: 
				Set<String> addresses = rData.getAddresses();
				serviceAndAddresses.put(serviceName, addresses);
				break;
			case ADD_ADDRESS: 
				serviceAndAddresses.get(serviceName).addAll(rData.getAddresses());
				break;
			case REMOVE_SERVICE: 
				serviceAndAddresses.remove(serviceName);
				break;
			case REMOVE_ADDRESS: 
				serviceAndAddresses.get(serviceName).removeAll(rData.getAddresses());
				break;
			default: 
				break;
			}
		}
	}

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
		}
	}
	
}
