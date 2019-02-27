package test.service;

import org.springframework.stereotype.Service;

import eva.client.core.context.Eva;
import test.TestInterface;

@Service
public class ClientService {
	
	public String doTest() {
		TestInterface intf = Eva.getService(TestInterface.class);
		String res = intf.testStr(System.currentTimeMillis());
		return res;
	}
	
}
