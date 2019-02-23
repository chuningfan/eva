package test.service;

import org.springframework.stereotype.Service;

import eva.client.core.context.Eva;
import eva.core.annotation.EvaCall;
import test.TestInterface;

@Service
public class ClientService {
	
	public void doTest() {
//		Eva.getService(TestInterface.class).test();
		@EvaCall TestInterface intf = Eva.getService(TestInterface.class);
		String res = intf.testStr(System.currentTimeMillis());
		System.out.println(res);
	}
	
}
