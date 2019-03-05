package test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eva.core.annotation.EvaDoll;
import eva.core.annotation.EvaService;

@EvaService(interfaceClass = TestInterface.class)
@Service
public class TestService implements TestInterface {

	@Autowired
	private TestService2 testService2;
	
	@Override
	@EvaDoll
	public void test() {
		testService2.test();
		System.out.println(11111);
	}

	@Override
	public String testStr(long l) {
		return "Hello World!" + l;
	}

}
