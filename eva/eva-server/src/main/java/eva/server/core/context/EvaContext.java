package eva.server.core.context;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Objects;
import java.util.Observable;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import eva.common.global.ProviderMetadata;
import eva.common.global.StatusEvent;
import eva.common.util.SPIServiceLoader;
import eva.core.base.BaseContext;
import eva.core.base.ResourceProvider;
import eva.core.base.config.ServerConfig;
import eva.core.exception.EvaContextException;
import eva.core.listener.StatusListener;
import eva.server.core.jmx.EvaMXAgent;
import eva.server.core.monitor.MonitorDataServer;
import eva.server.core.server.NioServer;

public class EvaContext extends BaseContext<ServerConfig> {

	private static final Logger LOG = LoggerFactory.getLogger(EvaContext.class);
	
	private static NioServer SERVER = null;

	private static ResourceProvider provider;
	
	private static MonitorDataServer MONITOR_SERVER;

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
							LOG.info("Eva started!");
							if (parameter.isJmxSupport()) {
								try {
									new EvaMXAgent(InetAddress.getLocalHost().getHostAddress(), parameter.getPort(), providerMetadata.getServices());
								} catch (MalformedObjectNameException | InstanceAlreadyExistsException
										| MBeanRegistrationException | NotCompliantMBeanException | IOException e) {
									e.printStackTrace();
									LOG.warn("When initializing JMX for eva, occurred an error. skip.");
								}
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
					Collection<Class<?>> interfaces = provider.getEvaInterfaceClasses();
					if (Objects.nonNull(interfaces) && !interfaces.isEmpty()) {
						Set<String> interfaceClassNameSet = interfaces.stream().map(Class::getName)
								.collect(Collectors.toSet());
						interfaceClassNameSet.stream().forEach(providerMetadata.getServices()::add);
					}
					if (needToRegister) {
						LOG.info("Registering local server to service registry");
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
						doRegister(parameter, providerMetadata);
					}
					SERVER.start();
					if (parameter.isMonitorSupport()) {
						if (Objects.isNull(MONITOR_SERVER)) {
							parameter.setDaemonName("Monitor-Data-Server-");
							MONITOR_SERVER = new MonitorDataServer(parameter);
							MONITOR_SERVER.start();
						}
					}
				}
			}
		}
	}

	public static final Set<String> getLocalInterfaces() {
		Set<String> set = Sets.newHashSet();
		Collection<Class<?>> collection = provider.getEvaInterfaceClasses();
		if (Objects.nonNull(collection) && !collection.isEmpty()) {
			collection.forEach(i -> {
				set.add(i.getName());
			});
		}
		return set;
	}
	
}
