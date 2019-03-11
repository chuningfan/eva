package eva.server.core.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.alibaba.fastjson.JSON;
import com.esotericsoftware.minlog.Log;
import com.google.common.collect.Sets;

import eva.common.monitor.EvaDataForMonitor;
import eva.common.monitor.EvaService;
import eva.common.registry.Registry;
import eva.core.base.config.ServerConfig;
import eva.server.core.context.EvaContext;

public class MonitorHelper {

	private static final ThreadMXBean tb = ManagementFactory.getThreadMXBean();
	private static final MemoryMXBean mb = ManagementFactory.getMemoryMXBean();
	private static final RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();

	private static final ExecutorService ES = Executors.newFixedThreadPool(3);
	
	private static ServerConfig config = null;
	
	public static final void startAndProcess(ServerConfig config) throws IOException {
		MonitorHelper.config = config;
		@SuppressWarnings("resource")
		ServerSocket ss = new ServerSocket(config.getMonitorSupportPort());
		while (true) {
			Socket socket = ss.accept();
			MonitorHandler handler = new MonitorHandler(socket);
			ES.execute(handler);
		}
	}

	private static final class MonitorHandler implements Runnable {
		private Socket client;
		public MonitorHandler(Socket client) {
			this.client = client;
		}
		@Override
		public void run() {
			InputStream in = null;
				try {
					in = client.getInputStream();
					InputStreamReader isr = new InputStreamReader(in, "utf-8");
	                BufferedReader br = new BufferedReader(isr);
	                String msg = null;
	                while ((msg = br.readLine()) != null) {
	                	String data = null;
						if ("watch".equalsIgnoreCase(msg)) {
							data = prepareData();
						}
						if ("service".equalsIgnoreCase(msg)) {
							Map<String, Set<String>> registryData = Registry.REGISTRY_DATA;
							Set<EvaService> set = Sets.newHashSet();
							if (Objects.nonNull(registryData) && !registryData.isEmpty()) {
								for (String serviceName: registryData.keySet()) {
									EvaService es = new EvaService();
									es.setServiceName(serviceName);
									es.setAddresses(registryData.get(serviceName));
									set.add(es);
								}
							} else {
								Set<String> interfaces = EvaContext.getLocalInterfaces();
								if (Objects.nonNull(interfaces) && !interfaces.isEmpty()) {
									interfaces.forEach(i -> {
										EvaService es = new EvaService();
										es.setServiceName(i);
										try {
											es.setAddresses(Sets.newHashSet(InetAddress.getLocalHost().getHostAddress() + ":" + config.getPort()));
										} catch (UnknownHostException e) {
											e.printStackTrace();
										}
										set.add(es);
									});
								}
							}
							data = JSON.toJSONString(set);
						}
						OutputStream os = client.getOutputStream();
						PrintWriter writer = new PrintWriter(os, true);
						writer.println(data);
	                }
				} catch (Exception e) {
					Log.error("Monitor Server:" + e.getMessage());
				}
			}

	}

	private static String prepareData() {
		int threadCount = tb.getThreadCount();
		int peakThreadCount = tb.getPeakThreadCount();
		// ---------------------------------------------------------
		long usedHeap = mb.getHeapMemoryUsage().getUsed();
		long usedNonHeap = mb.getNonHeapMemoryUsage().getUsed();
		long maxHeap = mb.getHeapMemoryUsage().getMax();
		// ---------------------------------------------------------
		long startTime = rb.getStartTime();
		long upTime = rb.getUptime();
		EvaDataForMonitor data = new EvaDataForMonitor();
		data.setMaxHeap(maxHeap);
		data.setPeakThreadCount(peakThreadCount);
		data.setThreadCount(threadCount);
		data.setStartTime(new Date(startTime));
		data.setTime(new Date());
		data.setUpTime(new Date(startTime + upTime));
		data.setUsedHeap(usedHeap);
		data.setUsedNonHeap(usedNonHeap);
		return JSON.toJSONString(data);
	}

}
