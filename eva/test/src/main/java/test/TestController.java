package test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

	@Autowired
	private TestInterface testInterface;
	
	@GetMapping("t")
	public String test() {
		testInterface.test();
		return "123";
	}
	
}
