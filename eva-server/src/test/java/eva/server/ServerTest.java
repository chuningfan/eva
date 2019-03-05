package eva.server;

import java.util.Scanner;

import eva.core.base.config.ServerConfig;
import eva.server.core.context.EvaContext;

public class ServerTest {
	
	public static void main(String[] args) {
		EvaContext ctx = new EvaContext(new ServerConfig());
		Scanner scan = new Scanner(System.in);
		scan.nextLine();
	}
	
}
