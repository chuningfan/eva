package eva.core.base;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;

import eva.common.global.ProviderMetadata;

public interface RegistryProvider<P> {

	Iterable<String> getAddressesByServiceName(String interfaceClassName);
	
	void doRegister(P parameter, ProviderMetadata metadata) throws IOException, KeeperException, InterruptedException;
	
	
	
}
