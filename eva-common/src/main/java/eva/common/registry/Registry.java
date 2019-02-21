package eva.common.registry;

import java.util.Observable;

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
	
}
