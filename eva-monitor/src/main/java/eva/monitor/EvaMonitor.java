package eva.monitor;

import java.util.Scanner;

public class EvaMonitor {
private static final Scanner scanner = new Scanner(System.in);
	
	public static void main(String[] args) {
		exeCommand();
	}
	
	static void exeCommand() {
		String cmd = null;
		String flag = "======Hello Eva!======";
		System.out.println(flag);
		while (!"exit".equalsIgnoreCase(cmd)) {
			System.out.print("EvaMonitor: ");
			cmd = scanner.nextLine();
			switch (cmd) {
			case "-help": 
				System.out.println("-log: To get target server RPC log.");
				System.out.println("-see: To watch target server RPC dynamic information.");
				System.out.println("-cfg: To get the configuration information of current monitor.");
				break;
			case "-log": 
				System.out.println("LOG: xxxxxxxxx");
				break;
			case "-see": 
				System.out.println("Loading ...");
				break;
			case "-cfg": 
				System.out.println("Loading ...");
				break;
			}
		}
		System.out.print("ByeBye Eva ...");
	} 
}
