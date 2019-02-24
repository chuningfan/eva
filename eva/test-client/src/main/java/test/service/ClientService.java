package test.service;

import org.springframework.stereotype.Service;

import eva.client.core.context.Eva;
import eva.client.core.dto.SpecifiedConfig;
import test.TestInterface;

@Service
public class ClientService {
	
	public void doTest() {
		TestInterface intf = Eva.getService(TestInterface.class, new SpecifiedConfig() {
			@Override
			public int getTimeout() {
				return 30;
			}
		});
		String res = intf.testStr(System.currentTimeMillis());
		System.out.println(res);
	}
	
}
