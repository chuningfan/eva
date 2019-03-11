package eva.common.monitor;

import java.util.Set;

public class EvaService {
	
	private String serviceName;
	
	private Set<String> addresses;

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public Set<String> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<String> addresses) {
		this.addresses = addresses;
	}
	
}
