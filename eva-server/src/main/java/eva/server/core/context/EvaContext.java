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
import eva.common.util.SPIServiceLoader;
import eva.core.base.BaseContext;
import eva.core.base.ResourceProvider;
import eva.core.base.config.ServerConfig;
import eva.core.exception.EvaContextException;
import eva.core.listener.StatusListener;
import eva.server.core.server.NioServer;

public class EvaContext extends BaseContext<ServerConfig> {

	private static NioServer SERVER = null;

	private final ResourceProvider provider;

	private ProviderMetadata providerMetadata;

	public EvaContext(ServerConfig config) throws EvaContextException, InterruptedException, IOException, KeeperException {
		super(config);
		// load SPI
		provider = SPIServiceLoader.getServiceInstanceOrDefault(ResourceProvider.class, null);
		config.setProvider(provider);
		init();
	}

	@Override	
	protected void init() throws EvaContextException, InterruptedException, IOException, KeeperException {
		if (Objects.isNull(SERVER)) {
			synchronized (this) {
				if (Objects.isNull(SERVER)) {
					providerMetadata = new ProviderMetadata();
					SERVER = new NioServer(parameter, providerMetadata);
					boolean needToRegister = Objects.nonNull(parameter.getRegistryAddress())
							&& !parameter.getRegistryAddress().trim().isEmpty();
					SERVER.addObserver(new StatusListener() {
						@Override
						public void onSuccess(Observable source, StatusEvent event) {
							try {
								Thread.sleep(500L);
								LOG.info("Registering local server to service registry");
							} catch (InterruptedException e) {
								LOG.info("Delay register eva server to registry failed, skip.");
							}
						}
						@Override
						public void onFailure(Observable source, StatusEvent event) {
							LOG.error("Eva encountered an error, cannot provide RPC service any more. " + event.getExc().getMessage());
						}
						@Override
						public void onClose(Observable source, StatusEvent event) {
							LOG.error("Eva is shutted down, cannot provide RPC service any more.");
						}
					});
					if (needToRegister) {
						addObserver(new StatusListener() {
							@Override
							public void update(Observable arg0, Object arg1) {
							}
							@Override
							public void onSuccess(Observable source, StatusEvent event) {
								LOG.info("Server has been registered on REGISTRY!");
							}
							@Override
							public void onFailure(Observable source, StatusEvent event) {
								LOG.error("Occurred an error, when registering server on REGISTRY!");
							}
						});
						Collection<Class<?>> interfaces = provider.getEvaInterfaceClasses();
						if (Objects.nonNull(interfaces) && !interfaces.isEmpty()) {
							Set<String> interfaceClassNameSet = interfaces.stream().map(Class<?>::getName)
									.collect(Collectors.toSet());
							interfaceClassNameSet.stream().forEach(providerMetadata.getServices()::add);
							doRegister(parameter, providerMetadata);
						}
					}
					SERVER.start();
				}
			}
		}
	}

}
