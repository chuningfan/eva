package test;

public class Tester {
	
	public void test() {
		
	}
	
	public static void main(String[] args) throws NoSuchMethodException, SecurityException {
//		ServerConfig config = new ServerConfig();
//		config.setPort(8080);
//		config.setServerId("testserver1");
//		config.setSpringApp(false);
//		NioServer server = new NioServer(config, null);
//		server.start();
//		Scanner scan = new Scanner(System.in);
//		String cmd = scan.nextLine();
//		while (!"bye".equalsIgnoreCase(cmd)) {
//			cmd = scan.nextLine();
//		}
		Class<?> cz = Tester.class.getDeclaredMethod("test").getReturnType();
		System.out.println(Tester.class.getDeclaredMethod("test").getReturnType());
	}
	
}
