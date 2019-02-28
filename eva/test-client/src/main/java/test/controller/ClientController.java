package test.controller;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.haiyiyang.server.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.haiyiyang.light.constant.LightConstants;
import com.haiyiyang.light.rpc.LightRpcContext;
import com.haiyiyang.light.service.proxy.LightServiceFactory;

import test.service.ClientService;

@RestController
@RequestMapping("/test")
public class ClientController {
	@Autowired
	private ClientService clientService;
	
	@GetMapping("c")
	public String test() {
		return clientService.doTest();
//		System.setProperty("useLocalProps", LightConstants.STR1);
//		UserService userService = LightServiceFactory.getService(UserService.class);
//		String helloResult = userService.hello(UUID.randomUUID().toString());
////
////		final UserService asyncUserService = LightServiceFactory.getAsyncService(UserService.class);
////		Future<String> welcomeFuture = LightRpcContext.getContext().asyncCall(asyncUserService, new Callable<String>() {
////			public String call() throws Exception {
////				return asyncUserService.welcome("San", "Zhang");
////			}
////		});
////		String welcomeResult = userService.welcome("Jack", "Ma");
////		try {
////			String asyncWelcomeResult = welcomeFuture.get();
////		} catch (InterruptedException e) {
////			e.printStackTrace();
////		} catch (ExecutionException e) {
////			e.printStackTrace();
////		}
//		return helloResult;
	}
}
