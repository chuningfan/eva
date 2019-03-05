package eva.server.core.context;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Observable;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.zookeeper.KeeperException;

import eva.common.global.ProviderMetadata;
import eva.common.global.StatusEvent;
import eva.common.registry.Registry;
import eva.common.util.SPIServiceLoader;
import eva.core.base.ResourceProvider;
import eva.core.base.config.ServerConfig;
import eva.core.listener.StatusListener;
import eva.server.core.server.NioServer;

public class EvaContext {

	private static NioServer SERVER = null;

	@SuppressWarnings("unused")
	private final ServerConfig config;

	private final ResourceProvider provider;

	private ProviderMetadata providerMetadata;

	public EvaContext(ServerConfig config) {
		// load SPI
		provider = SPIServiceLoader.getServiceInstanceOrDefault(ResourceProvider.class, null);
		config.setProvider(provider);
		this.config = config;
		init(config);
	}

	private void init(ServerConfig config) {
		if (Objects.isNull(SERVER)) {
			synchronized (this) {
				if (Objects.isNull(SERVER)) {
					providerMetadata = new ProviderMetadata();
					SERVER = new NioServer(config, providerMetadata);
					boolean needToRegister = Objects.nonNull(config.getRegistryAddress())
							&& !config.getRegistryAddress().trim().isEmpty();
					SERVER.addObserver(new StatusListener() {
						@Override
						public void onSuccess(Observable source, StatusEvent event) {
							try {
								Thread.sleep(500L);
								LOG.info("Registering local server to service registry");
							} catch (InterruptedException e) {
								LOG.info("Delay register eva server to registry failed, skip.");
							}
							// TODO register local host to registry
							Registry.get().addObserver(new StatusListener() {
								@Override
								public void onSuccess(Observable source, StatusEvent event) {
									LOG.info("Eva has beean registered on the registry");
								}

								@Override
								public void onFailure(Observable source, StatusEvent event) {
									LOG.info("Cannot register Eva to registry, RPC is unavailable.");
								}
							});
							try {
								Registry.get().registerServerToRegistry(config.getRegistryAddress(), providerMetadata);
							} catch (IOException | KeeperException | InterruptedException e1) {
								LOG.warning("Cannot register Eva to registry, RPC is unavailable. " + e1.getMessage());
							}
							String registryAddress = config.getRegistryAddress();
							if (Objects.isNull(registryAddress) || "".equals(registryAddress.trim())) {
								LOG.info("No registry address is provided, eva is cannot provide RPC service.");
							} else {
								Registry.get().addObserver(new StatusListener() {
									@Override
									public void onSuccess(Observable source, StatusEvent event) {
										LOG.info("Provider [" + config.getServerId() + "] registered!");
									}

									@Override
									public void onFailure(Observable source, StatusEvent event) {
										Throwable e = event.getExc();
										LOG.info("Failed to register provider: " + e.getMessage());
									}
								});
							}
						}

						@Override
						public void onFailure(Observable source, StatusEvent event) {
							LOG.warning("Eva encountered an error, cannot provide RPC service any more.");
						}

						@Override
						public void onClose(Observable source, StatusEvent event) {
							LOG.warning("Eva is shutted down, cannot provide RPC service any more.");
						}
					});
					if (needToRegister) {
						Collection<Class<?>> interfaces = provider.getEvaInterfaceClasses();
						if (Objects.nonNull(interfaces) && !interfaces.isEmpty()) {
							Set<String> interfaceClassNameSet = interfaces.stream().map(Class<?>::getName)
									.collect(Collectors.toSet());
							interfaceClassNameSet.stream().forEach(providerMetadata.getServices()::add);
						}	
					}
					SERVER.start();
				}
			}
		}
	}

}
