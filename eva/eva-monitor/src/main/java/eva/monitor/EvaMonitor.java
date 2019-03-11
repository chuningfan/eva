package eva.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import eva.common.monitor.EvaDataForMonitor;
import eva.common.monitor.EvaService;

public class EvaMonitor {
	
	private static final Scanner scanner = new Scanner(System.in);
	
	private static Socket client = null;
	
	private static ServerConfigDto CONFIG = null;
	
	static {
		String basePath = EvaMonitor.class.getClassLoader().getResource("").getPath();
		CONFIG = new ServerConfigDto();
		CONFIG.setPort(8220);
		CONFIG.setIp("127.0.0.1");
	}
	
	public static void main(String[] args) throws Exception {
		exeCommand();
	}
	
	private static Socket createClientIfNecessary() throws IOException {
		if (client == null) {
			client = Client.getConnection(new InetSocketAddress(CONFIG.getIp(), CONFIG.getPort()));
			client.setKeepAlive(true);
			client.setTcpNoDelay(true);
		}
		return client;
	}
	
	private static void exeCommand() throws Exception {
		String cmd = null;
		String flag = "======Hello Eva!======";
		System.out.println(flag);
		Command command = canonicalCommand(cmd);
		while (command != Command.EXIT) {
			System.out.print("EvaMonitor: ");
			cmd = scanner.nextLine();
			command = canonicalCommand(cmd);
			command.process();
		}
	} 
	
	static Command canonicalCommand(String command) {
		Predicate<String> p = new Predicate<String>() {
			@Override
			public boolean test(String t) {
				Command[] array = Command.values();
				for (Command c: array) {
					if (c.name().equalsIgnoreCase(t))
						return true;
				}
				return false;
			}
		};
		if (!simpleCheck(command) || Stream.of(command).anyMatch(p)) {
			return Command.ERROR;
		}
		command = command.replace("-", "").toUpperCase();
		return Command.valueOf(command);
	}
	
	static boolean simpleCheck(String command) {
		if (Objects.isNull(command) || command.length() < 1) {
			return false;
		}
		Pattern p = Pattern.compile("^-.{0,1}([a-z]|[A-Z]).*$");
		Matcher m = p.matcher(command);
		return m.matches();
	}
	
	enum Command {
		ERROR() {
			@Override
			protected void process() {
				System.out.println("Invalid command, please input -help to get more information.");
			}
		},
		EXIT() {
			@Override
			protected void process() throws IOException {
				if (client != null) {
					client.close();
				}
				scanner.close();
				System.out.println("ByeBye Eva ...");
			}
		},
		WATCH() {
			@Override
			protected void process() throws Exception {
				String msg = getFeedback(sendCommand("watch"));
				EvaDataForMonitor ed = JSON.parseObject(msg, EvaDataForMonitor.class);
				if (Objects.nonNull(ed)) {
					System.out.println(JSON.toJSONString(ed, true));
				} else {
					System.out.println("Cannot get any information for EVA server");
				}
			}
		},
		SERVICE() {
			@Override
			protected void process() throws Exception {
				String msg = getFeedback(sendCommand("service"));
				@SuppressWarnings("unchecked")
				Set<EvaService> set = JSON.parseObject(msg, HashSet.class);
				if (Objects.nonNull(set) && !set.isEmpty()) {
					System.out.println(JSON.toJSONString(set, true));
				} else {
					System.out.println("Cannot get any information for services");
				}
			}
		},
		HELP() {
			@Override
			protected void process() {
				String helpString = "-watch: to see the eva server information \n"
						+ "-service: to see all services which are registered on registry";
				System.out.println(helpString);
			}
		};
		protected abstract void process() throws Exception;
		
		private static String getFeedback(Socket client) throws IOException {
			InputStream in = client.getInputStream();
			BufferedReader bis = new BufferedReader(new InputStreamReader(in));
			String msg = null;
			while ((msg = bis.readLine()) != null) {
				return msg;
			}
			return null;
		}
		
		private static Socket sendCommand(String command) throws IOException {
			Socket client = createClientIfNecessary();
			OutputStream os = client.getOutputStream();
			PrintWriter writer = new PrintWriter(os, true);
			writer.println(command);
			return client;
		}
	}
	
	
}
