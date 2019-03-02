package test;

import eva.core.annotation.EvaEndpoint;
import eva.core.annotation.EvaService;

@EvaService(maximumConcurrency=1)
public class TestService2 implements TestInterface2 {

	@Override
	@EvaEndpoint
	public void test() {
		System.out.println(22222);
	}

}
