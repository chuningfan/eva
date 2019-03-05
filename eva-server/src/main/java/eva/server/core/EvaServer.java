package eva.server.core;

import java.io.IOException;
import java.util.Scanner;

import org.apache.zookeeper.KeeperException;

import eva.core.base.config.ServerConfig;
import eva.core.exception.EvaContextException;
import eva.server.core.context.EvaContext;

public class EvaServer {
	
	public static void main(String[] args) throws EvaContextException, InterruptedException, IOException, KeeperException {
		ServerConfig config = new ServerConfig();
		EvaContext ctx = new EvaContext(config);
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
	}
	
}
