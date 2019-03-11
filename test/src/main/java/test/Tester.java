package test;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import eva.core.base.config.ServerConfig;
import eva.core.exception.EvaContextException;
import eva.server.core.context.EvaContext;

@SpringBootApplication
public class Tester {
	
	public static void main(String[] args) {
		SpringApplication.run(Tester.class, args);
	}
	
//	@Bean
//	public AncientContext ancientContext() throws Throwable {
//		ServerConfig config = new ServerConfig();
//		config.setBossSize(1);
//		config.setWorkerSize(Runtime.getRuntime().availableProcessors());
////		config.setAsyncProcessing(true);
////		config.setRegistryAddress("127.0.0.1:2181");
//		return new AncientContext(config);
//	}
	
	@Bean
	public EvaContext evaContext() throws EvaContextException, InterruptedException, IOException, KeeperException {
		ServerConfig config = new ServerConfig();
		config.setBossSize(2);
		config.setWorkerSize(Runtime.getRuntime().availableProcessors() * 2);
//		config.setAsyncProcessing(true);
		config.setJmxSupport(true);
//		config.setRegistryAddress("192.168.129.130:2181");
		config.setRegistryAddress("127.0.0.1:2181");
		config.setMonitorSupport(true);
		return new EvaContext(config);
	}
	
}
