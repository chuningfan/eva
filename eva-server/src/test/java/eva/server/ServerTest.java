package eva.server;

import java.io.IOException;
import java.util.Scanner;

import org.apache.zookeeper.KeeperException;

import eva.core.base.config.ServerConfig;
import eva.core.exception.EvaContextException;
import eva.server.core.context.EvaContext;

public class ServerTest {
	
	public static void main(String[] args) throws EvaContextException, InterruptedException, IOException, KeeperException {
		EvaContext ctx = new EvaContext(new ServerConfig());
		Scanner scan = new Scanner(System.in);
		scan.nextLine();
	}
	
}
