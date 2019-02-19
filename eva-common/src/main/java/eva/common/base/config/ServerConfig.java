package eva.common.base.config;

public class ServerConfig extends BaseConfiguration {
	
	private int bossSize = 1;
	
	private int workerSize = Runtime.getRuntime().availableProcessors();
	
	private String[] scanPackages;
	
	private boolean isSpringApp;

	public int getBossSize() {
		return bossSize;
	}

	public void setBossSize(int bossSize) {
		this.bossSize = bossSize;
	}

	public int getWorkerSize() {
		return workerSize;
	}

	public void setWorkerSize(int workerSize) {
		this.workerSize = workerSize;
	}

	public String[] getScanPackages() {
		return scanPackages;
	}

	public void setScanPackages(String[] scanPackages) {
		this.scanPackages = scanPackages;
	}

	public boolean isSpringApp() {
		return isSpringApp;
	}

	public void setSpringApp(boolean isSpringApp) {
		this.isSpringApp = isSpringApp;
	}
	
}
