package eva.common.registry;

import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import eva.common.dto.ProviderMetadata;

public class Registry extends Observable {
	
	private static final class RegistryHolder {
		private static final Registry INSTANCE = new Registry();
	}

	public static final Registry get() {
		return RegistryHolder.INSTANCE;
	}
	
	public final void registerServerToRegistry(String registryAddress, ProviderMetadata providerMetadata) {
		
	}
	
	public final List<String> getAddresses(String providerName) {
		return null;
	}
	
	public final Map<String, Set<String>> getAllNodes() {
		return null;
	}
	
}
