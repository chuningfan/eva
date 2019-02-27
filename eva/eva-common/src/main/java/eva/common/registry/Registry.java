package eva.common.registry;

import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import eva.common.global.ProviderMetadata;
import eva.common.global.StatusEvent;

public class Registry extends Observable implements Watcher {
	
	private static final class RegistryHolder {
		private static final Registry INSTANCE = new Registry();
	}

	public static final Registry get() {
		return RegistryHolder.INSTANCE;
	}
	
	public final void registerServerToRegistry(String registryAddress, ProviderMetadata providerMetadata) {
		StatusEvent event = null;
		try {
			event = StatusEvent.getStartupEvent();
		} catch (Exception e) {
			event = StatusEvent.getFailedEvent(e);
		}
		setChanged();
		notifyObservers(event);
	}
	
	public final List<String> getAddresses(String providerName) {
		return null;
	}
	
	public final Map<String, Set<String>> getAllNodes() {
		return null;
	}

	@Override
	public void process(WatchedEvent event) {
		switch (event.getState()) {
		case Disconnected: 
			
			break;
		case SyncConnected: 
			break;
		default: break;
		}
	}
	
}
