package eva.client.core;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import com.google.common.collect.Maps;

import eva.common.base.Pool;
import eva.common.base.config.ClientConfig;
import io.netty.channel.socket.SocketChannel;

public class ClientProvider implements Pool<SocketChannel> {

	private static volatile Map<String, LinkedList<SocketChannel>> POOL = Maps.newConcurrentMap();

	@Override
	public SocketChannel getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeSource(SocketChannel target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<SocketChannel> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SocketChannel getSource(String serverAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<SocketChannel> getSources(String serverAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SocketChannel create(ClientConfig config, String serverAddress) {
		
		return null;
	}
	
	
}
