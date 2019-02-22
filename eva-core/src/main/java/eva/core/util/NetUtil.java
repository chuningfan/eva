package eva.core.util;

import java.net.InetSocketAddress;

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
	
}
