package eva.common.dto;

import java.util.List;

public class ProviderMetadata {

	private String providerName;

	private List<ServiceMetadata> serviceInfos;

	private String host;

	private int port;

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public List<ServiceMetadata> getServiceInfos() {
		return serviceInfos;
	}

	public void setServiceInfos(List<ServiceMetadata> serviceInfos) {
		this.serviceInfos = serviceInfos;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
