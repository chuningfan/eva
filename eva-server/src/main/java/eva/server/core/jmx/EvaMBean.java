package eva.server.core.jmx;

import java.util.Map;
import java.util.Set;

public interface EvaMBean {
	
	String getIp();
	void setIp(String ip);
	int getPort();
	void setPort(int port);
	Set<String> getEvaServices();
	Map<String, Set<String>> getRegistryInfo();
	Set<String> getInterfaceAddresses(String interfaceName);
	
}
