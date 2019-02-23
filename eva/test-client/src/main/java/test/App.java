package test;

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
	public EvaClientContext evaClientContext() throws EvaContextException {
		ClientConfig config = new ClientConfig();
		config.setClientId(1L);
		config.setGlobalTimoutMilliSec(30000);
		config.setMaxSizePerProvider(50);
		config.setSingleHostAddress("127.0.0.1:8763");
		return new EvaClientContext(config);
	}
	
}
