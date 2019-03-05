package test;

import org.springframework.stereotype.Service;

import eva.core.annotation.EvaDoll;
import eva.core.annotation.EvaService;

@EvaService(interfaceClass=TestInterface2.class)
@Service
public class TestService2 implements TestInterface2 {

	@Override
	@EvaDoll
	public void test() {
		System.out.println(22222);
	}

}
