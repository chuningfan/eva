package eva.server.core.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class EvaMXAgent {
	
	public EvaMXAgent(String ip, int port, Set<String> evaServices) throws MalformedObjectNameException, 
		InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, 
		IOException {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		String domainName = "EvaMX";
		ObjectName name = new ObjectName(domainName+":name=serverInfo");
		Eva emx = new Eva();
		emx.setIp(ip);
		emx.setPort(port);
		emx.setEvaServices(evaServices);
		server.registerMBean(emx,name);
	}
	
}
