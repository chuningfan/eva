package eva.client.core.context;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eva.common.base.Pool;
import eva.common.exception.EvaClientException;
import eva.common.transport.codec.NioServerDecoder;
import eva.common.transport.codec.NioServerEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

class ClientProvider implements Pool<SocketChannel> {

	// key: address, value: channels
	private volatile Map<String, LinkedList<Channel>> POOL = Maps.newConcurrentMap();

	private ReentrantLock lock = new ReentrantLock();

	private static final class ClientProviderHolder {
		private static final ClientProvider INSTANCE = new ClientProvider();
	}

	public static final ClientProvider get() {
		return ClientProviderHolder.INSTANCE;
	}

	private boolean isSingleHost;

	private String serverAddress;

	private int maxSizePerProvider;

	@Override
	public SocketChannel getSource() {

		return null;
	}

	@Override
	public void removeSource(SocketChannel target) {

	}

	@Override
	public void clear() {

	}

	@Override
	public Collection<SocketChannel> getAll() {

		return null;
	}

	@Override
	public SocketChannel getSource(String serverAddress) {

		return null;
	}

	@Override
	public Collection<SocketChannel> getSources(String serverAddress) {

		return null;
	}

	private String getChannelAddress(String providerName) throws EvaClientException {
		if (isSingleHost || Objects.isNull(providerName)) {
			return serverAddress;
		} else {
			Set<String> addresses = EvaClientContext.REGISTRY_DATA == null ? null : EvaClientContext.REGISTRY_DATA.get(providerName);
			if (Objects.isNull(addresses)) {
				throw new EvaClientException("Cannot get registry data!");
			}
			Set<Entry<String, LinkedList<Channel>>> set = POOL.entrySet().stream().filter(e -> addresses.contains(e.getKey())).collect(Collectors.toSet());
			Optional<Entry<String, LinkedList<Channel>>> opt = set.stream().min(new Comparator<Entry<String, LinkedList<Channel>>>() {
				@Override
				public int compare(Entry<String, LinkedList<Channel>> arg0, Entry<String, LinkedList<Channel>> arg1) {
					int a = arg0.getValue() == null ? 0 : arg0.getValue().size();
					int b = arg1.getValue() == null ? 0 : arg1.getValue().size();
					if (a < b) {
						return -1;
					} else if (b < a) {
						return 1;
					} else {
						return 0;
					}
				}
			});
			return opt.get().getKey();
		}
	}

	@Override
	public Channel create(String providerName) throws EvaClientException {
		String channelAddr = getChannelAddress(providerName);
		String host = channelAddr.split(":")[0];
		int port = Integer.parseInt(channelAddr.split(":")[1]);
		if (Objects.isNull(providerName)) {
			LinkedList<Channel> channels = POOL.get(channelAddr);
			try {
				lock.lock();
				if (Objects.nonNull(channels) && !channels.isEmpty()) {
					return channels.poll();
				} else {
					POOL.put(channelAddr, Lists.newLinkedList());
				}
			} finally {
				lock.unlock();
			}
		}
		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
				.option(ChannelOption.TCP_NODELAY, Boolean.TRUE).handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast(new NioServerEncoder());
						pipeline.addLast(new NioServerDecoder());
					}
				});
		ChannelFuture channelFuture = bootstrap.connect(host, port);
		Channel channel = null;
		try {
			channel = channelFuture.sync().channel();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return channel;
	}

	public boolean isSingleHost() {
		return isSingleHost;
	}

	public void setSingleHost(boolean isSingleHost) {
		this.isSingleHost = isSingleHost;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public int getMaxSizePerProvider() {
		return maxSizePerProvider;
	}

	public void setMaxSizePerProvider(int maxSizePerProvider) {
		this.maxSizePerProvider = maxSizePerProvider;
	}

	public static void main(String[] args) {
		
	}
	
}
