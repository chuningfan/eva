package test;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import eva.client.core.context.EvaClientContext;
import eva.core.base.config.ClientConfig;
import eva.core.exception.EvaContextException;

@SpringBootApplication
public class App {
	
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
	
	@Bean
	public EvaClientContext evaClientContext() throws EvaContextException, InterruptedException, KeeperException, IOException {
		ClientConfig config = new ClientConfig();
		config.setClientId(1L);
		config.setGlobalTimoutMilliSec(30000);
		config.setCoreSizePerHost(20);
		config.setSingleHostAddress("127.0.0.1:8763");
//		config.setRegistryAddress("192.168.129.130:2181");
		return new EvaClientContext(config);
	}
	
}
