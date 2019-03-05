package eva.common.registry;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Set;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eva.common.global.ProviderMetadata;
import eva.common.global.StatusEvent;

public class Registry extends Observable implements Watcher {

	private static final String ROOT = "/2017EVA0106";

	public static volatile Map<String, Set<String>> REGISTRY_DATA = Maps.newConcurrentMap();

	private ZooKeeper zk;
	
	private static final class RegistryHolder {
		private static final Registry INSTANCE = new Registry();
	}

	public static final Registry get() {
		return RegistryHolder.INSTANCE;
	}

	public final void registerServerToRegistry(String registryAddress, ProviderMetadata providerMetadata)
			throws IOException, KeeperException, InterruptedException {
		if (Objects.isNull(zk)) {
			zk = new ZooKeeper(registryAddress, 30 * 1000, this);
		}
		StatusEvent event = null;
		String providerAddress = providerMetadata.getHost() + ":" + providerMetadata.getPort();
		try {
			String path = null;
			for (String serviceName : providerMetadata.getServices()) {
				path = ROOT + "/" + serviceName;
				if (Objects.isNull(zk.exists(path, true))) {
					zk.create(path, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
				if (Objects.isNull(zk.exists(path + "/" + providerAddress, true))) {
					zk.create(path + "/" + providerAddress, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
							CreateMode.EPHEMERAL);
				}
				Set<String> addressSet = REGISTRY_DATA.get(serviceName);
				if (Objects.isNull(addressSet)) {
					addressSet = Sets.newHashSet();
					REGISTRY_DATA.put(serviceName, addressSet);
				}
				addressSet.add(registryAddress);
			}
			event = StatusEvent.getStartupEvent();
		} catch (Exception e) {
			event = StatusEvent.getFailedEvent(e);
		} finally {
			syncData(zk);
		}
		setChanged();
		notifyObservers(event);
	}

	public void syncData(ZooKeeper zk) throws KeeperException, InterruptedException {
		if (Objects.isNull(zk)) {
			this.zk = zk;
		}
		List<String> services = zk.getChildren(ROOT, true);
		if (Objects.nonNull(services) && !services.isEmpty()) {
			for (String serviceName : services) {
				List<String> addressList = zk.getChildren(ROOT + "/" + serviceName, true);
				Set<String> addressSet = REGISTRY_DATA.get(serviceName);
				if (Objects.isNull(addressSet)) {
					addressSet = Sets.newHashSet();
					REGISTRY_DATA.put(serviceName, addressSet);
				}
				if (Objects.nonNull(addressList) && !addressList.isEmpty()) {
					for (String addr : addressList) {
						addressSet.add(addr);
					}
				}
			}
		}
	}

	@Override
	public void process(WatchedEvent event) {
		if (Objects.nonNull(event.getPath())) {
			
		}
	}
}
