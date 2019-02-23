package test.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import test.service.ClientService;

@RestController
@RequestMapping("/test")
public class ClientController {
	@Autowired
	private ClientService clientService;
	
	@GetMapping("c")
	public void test() {
		clientService.doTest();
	}
}
