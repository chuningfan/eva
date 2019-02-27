package eva.common.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetUtil {
	
	
	public static final String getAddress(InetSocketAddress addr) {
		return addr.getAddress().getHostAddress() + ":" + addr.getPort();
	}
	
	public static final InetSocketAddress getAddress(String addr) {
		String[] arr = addr.split(":");
		String host = arr[0];
		int port = Integer.parseInt(arr[1]);
		return new InetSocketAddress(host, port);
	}
	
	public static final String getHost(String addr) {
		String[] arr = addr.split(":");
		return arr[0];
	}
	
	public static final int getPort(String addr) {
		String[] arr = addr.split(":");
		int port = Integer.parseInt(arr[1]);
		return port;
	}
	public static boolean pingHost(String ipAddress, int port) throws Exception {
		boolean isReachable = false;
		Socket connect = new Socket();
		try {
			InetSocketAddress endpointSocketAddr = new InetSocketAddress(ipAddress, port);
			connect.connect(endpointSocketAddr,3000);
			isReachable = connect.isConnected();
		} catch (Exception e) {
			throw new Exception(e.getMessage() + ", ip = " + ipAddress + ", port = " +port);
		} finally {
			try {
				connect.close();
			} catch (IOException e) {
				throw new Exception(e.getMessage() + ", ip = " + ipAddress + ", port = " +port);
			}
		}
		return isReachable;
	}
}
