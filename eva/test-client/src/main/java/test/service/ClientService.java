package test.service;

import org.springframework.stereotype.Service;

import eva.client.core.context.Eva;
import test.TestInterface;

@Service
public class ClientService {
	
	public void doTest() {
//		Eva.getService(TestInterface.class).test();
		
		String res = Eva.getService(TestInterface.class).testStr();
		System.out.println(res);
	}
	
}
