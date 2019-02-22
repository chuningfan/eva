package eva.core.base.config;

public class ClientConfig {
	
	private String strategy;
	
	private String registryAddress;
	
	private String singleHostAddress;
	
	private int maxSizePerProvider;
	
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
	
}
