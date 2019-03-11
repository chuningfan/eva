package eva.server.core.jmx;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import eva.common.registry.Registry;

public class Eva implements EvaMBean {

	private String ip;
	
	private int port;
	
	private Set<String> evaServices;
	
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Set<String> getEvaServices() {
		return evaServices;
	}

	public void setEvaServices(Set<String> evaServices) {
		this.evaServices = evaServices;
	}

	@Override
	public Map<String, Set<String>> getRegistryInfo() {
		return Registry.REGISTRY_DATA;
	}

	@Override
	public Set<String> getInterfaceAddresses(String interfaceName) {
		if (Objects.nonNull(Registry.REGISTRY_DATA)) {
			return Registry.REGISTRY_DATA.get(interfaceName);
		}
		return null;
	}

	
}
