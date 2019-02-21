package test;

import org.springframework.stereotype.Component;

import eva.common.annotation.EvaEndpoint;
import eva.common.annotation.EvaService;

@Component
@EvaService(maximumConcurrency=1)
public class TestService2 implements TestInterface2 {

	@Override
	@EvaEndpoint
	public void test() {
		System.out.println(22222);
	}

}
