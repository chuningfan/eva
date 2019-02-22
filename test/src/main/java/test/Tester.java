package test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import eva.core.base.config.ServerConfig;
import eva.server.core.context.AncientContext;

@SpringBootApplication
public class Tester {
	
	public static void main(String[] args) {
		SpringApplication.run(Tester.class, args);
	}
	
	@Bean
	public AncientContext ancientContext() throws Throwable {
		ServerConfig config = new ServerConfig();
		return new AncientContext(config);
	}
	
}
