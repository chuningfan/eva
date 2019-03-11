package eva.monitor;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

public class Client {
	
	static Socket getConnection(SocketAddress evaServerAddress) throws IOException {
		Socket s = new Socket();
		s.connect(evaServerAddress);
		return s;
	}
	
}
