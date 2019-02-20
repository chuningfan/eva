package test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eva.common.annotation.EvaEndpoint;
import eva.common.annotation.EvaService;

@Component
@EvaService(interfaceClass = TestInterface.class, maximumConcurrency=1)
public class TestService implements TestInterface {

	@Autowired
	private TestInterface2 testInterface2;
	
	@Override
	@EvaEndpoint
	public void test() {
		testInterface2.test();
		System.out.println(11111);
	}

}
