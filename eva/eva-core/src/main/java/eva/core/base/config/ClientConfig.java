package eva.core.base.config;

public class ClientConfig {
	
	private long clientId;
	
	private String strategy = "random";
	
	private String registryAddress;
	
	private String singleHostAddress;
	
	private int coreSizePerHost = 5;
	
	private long globalTimoutMilliSec = 3000;
	
	
	public long getClientId() {
		return clientId;
	}

	public void setClientId(long clientId) {
		this.clientId = clientId;
	}

	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	public String getRegistryAddress() {
		return registryAddress;
	}

	public void setRegistryAddress(String registryAddress) {
		this.registryAddress = registryAddress;
	}

	public String getSingleHostAddress() {
		return singleHostAddress;
	}

	public void setSingleHostAddress(String singleHostAddress) {
		this.singleHostAddress = singleHostAddress;
	}

	public long getGlobalTimoutMilliSec() {
		return globalTimoutMilliSec;
	}

	public void setGlobalTimoutMilliSec(long globalTimoutMilliSec) {
		this.globalTimoutMilliSec = globalTimoutMilliSec;
	}

	public int getCoreSizePerHost() {
		return coreSizePerHost;
	}

	public void setCoreSizePerHost(int coreSizePerHost) {
		this.coreSizePerHost = coreSizePerHost;
	}

}
