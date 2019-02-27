package eva.common.registry;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Set;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eva.common.global.ProviderMetadata;
import eva.common.global.StatusEvent;
import eva.common.util.NetUtil;

public class Registry extends Observable implements Watcher {
	
	private static final String ROOT = "/EVA-20170106";
	
	public static volatile Map<String, Set<String>> REGISTRY_DATA = Maps.newConcurrentMap();
	
	private static final class RegistryHolder {
		private static final Registry INSTANCE = new Registry();
	}

	public static final Registry get() {
		return RegistryHolder.INSTANCE;
	}
	
	public final void registerServerToRegistry(String registryAddress, ProviderMetadata providerMetadata) throws IOException {
		ZooKeeper zk = new ZooKeeper(NetUtil.getHost(registryAddress), NetUtil.getPort(registryAddress), this);
		StatusEvent event = null;
		try {
			String addr = providerMetadata.getHost() + ":" + providerMetadata.getPort();
			Set<String> set = REGISTRY_DATA.get(addr);
			if (Objects.isNull(set)) {
				set = Sets.newHashSet();
			}
			String path = null;
			for (String serviceName: providerMetadata.getServices()) {
				path = ROOT + "/" + serviceName;
				path = zk.create(path, addr.getBytes(), null, CreateMode.EPHEMERAL);
				set.add(path);
			}
			REGISTRY_DATA.put(addr, set);
			event = StatusEvent.getStartupEvent();
		} catch (Exception e) {
			event = StatusEvent.getFailedEvent(e);
		}
		setChanged();
		notifyObservers(event);
	}
	
	@Override
	public void process(WatchedEvent event) {
		switch (event.getState()) {
		case Disconnected: 
			removeNode(event);
			break;
		case SyncConnected: 
			addNode(event);
			break;
		default: break;
		}
	}
	
	private void addNode(WatchedEvent event) {
		String path = event.getPath();
		String[] nodePaths = path.split("/");
		Set<String> set = REGISTRY_DATA.get(nodePaths[1]);
		if (Objects.isNull(set)) {
			set = Sets.newHashSet();
			REGISTRY_DATA.put(nodePaths[1], set);
			
		}
		set.add(nodePaths[2]);
	}
	
	
	private void removeNode(WatchedEvent event) {
		String path = event.getPath();
		String[] nodePaths = path.split("/");
		Set<String> set = REGISTRY_DATA.get(nodePaths[1]);
		if (Objects.nonNull(set)) {
			set.remove(nodePaths[2]);
		}
	}
}
