package test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eva.core.annotation.EvaEndpoint;
import eva.core.annotation.EvaService;

@Component
@EvaService(interfaceClass = TestInterface.class, maximumConcurrency=1)
public class TestService implements TestInterface {

	@Autowired
	private TestService2 testService2;
	
	@Override
	@EvaEndpoint
	public void test() {
		testService2.test();
		System.out.println(11111);
	}

	@Override
	public String testStr(long l) {
		return "Hello World!" + l;
	}

}
