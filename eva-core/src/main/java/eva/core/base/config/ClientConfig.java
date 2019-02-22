package eva.core.base.config;

public class ClientConfig {
	
	private String strategy = "random";
	
	private String registryAddress;
	
	private String singleHostAddress;
	
	private int maxSizePerProvider = 5;
	
	private long globalTimoutMilliSec = 3000;
	
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

	public int getMaxSizePerProvider() {
		return maxSizePerProvider;
	}

	public void setMaxSizePerProvider(int maxSizePerProvider) {
		this.maxSizePerProvider = maxSizePerProvider;
	}

	public long getGlobalTimoutMilliSec() {
		return globalTimoutMilliSec;
	}

	public void setGlobalTimoutMilliSec(long globalTimoutMilliSec) {
		this.globalTimoutMilliSec = globalTimoutMilliSec;
	}

	
}
