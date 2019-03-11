package eva.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSON;
import com.esotericsoftware.minlog.Log;

import eva.common.monitor.EvaDataForMonitor;
import eva.common.monitor.EvaService;
import eva.common.util.CommonUtil;

public class EvaMonitor {

	private static final Scanner scanner = new Scanner(System.in);

	private static Socket client = null;

	private static ServerConfigDto CONFIG = null;

	private static boolean FLAG = true;

	private static final String EVA_SERVER_MONITOR_PORT_KEY = "eva.server.monitor.port";

	private static final String EVA_SERVER_Address_KEY = "eva.server.ip";

	static {
		String basePath = EvaMonitor.class.getClassLoader().getResource("").getPath();
		String configPath = basePath.replace("/bin/", "/config/cfg.properties");
		System.out.println(configPath);
		File file = new File(configPath);
		if (file.isFile()) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				Properties pro = new Properties();
				pro.load(in);
				CONFIG = new ServerConfigDto();
				CONFIG.setPort(Integer.parseInt(pro.getProperty(EVA_SERVER_MONITOR_PORT_KEY)));
				CONFIG.setIp(pro.getProperty(EVA_SERVER_Address_KEY));
			} catch (Exception e) {
				e.printStackTrace();
				FLAG = false;
			} finally {
				try {
					CommonUtil.closeStreams(in);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static Socket createClientIfNecessary() throws IOException {
		if (client == null) {
			client = Client.getConnection(new InetSocketAddress(CONFIG.getIp(), CONFIG.getPort()));
			client.setKeepAlive(true);
			client.setTcpNoDelay(true);
		}
		return client;
	}

	static void exeCommand() throws Exception {
		if (!FLAG) {
			Log.error("Monitor read configuration file failed, skip!");
			return;
		}
		String cmd = null;
		String flag = "======Hello Eva!======\n" + "Eva Server Address: " + CONFIG.getIp() + ":" + CONFIG.getPort();
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
				for (Command c : array) {
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
				try {
					String msg = getFeedback(sendCommand("watch"));
					EvaDataForMonitor ed = JSON.parseObject(msg, EvaDataForMonitor.class);
					if (Objects.nonNull(ed)) {
						System.out.println(JSON.toJSONString(ed, true));
					} else {
						System.out.println("Cannot get any information for EVA server");
					}
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			}
		},
		SERVICE() {
			@Override
			protected void process() throws Exception {
				try {
					String msg = getFeedback(sendCommand("service"));
					@SuppressWarnings("unchecked")
					Set<EvaService> set = JSON.parseObject(msg, HashSet.class);
					if (Objects.nonNull(set) && !set.isEmpty()) {
						System.out.println(JSON.toJSONString(set, true));
					} else {
						System.out.println("Cannot get any information for services");
					}
				} catch (IOException e) {
					System.out.println(e.getMessage());
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
