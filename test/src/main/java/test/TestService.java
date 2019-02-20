package test;

import org.springframework.stereotype.Component;

import eva.common.annotation.EvaEndpoint;
import eva.common.annotation.EvaService;

@Component
@EvaService(interfaceClass = TestInterface.class, maximumConcurrency=1)
public class TestService implements TestInterface {

	@Override
	@EvaEndpoint
	public void test() {
		System.out.println(123123);
	}

}
