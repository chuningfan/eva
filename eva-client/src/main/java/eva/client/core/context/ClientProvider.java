package eva.client.core.context;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Observable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eva.balance.strategies.BalanceStrategyFactory;
import eva.client.core.dto.ClientWrapper;
import eva.common.global.NodeChangeEvent;
import eva.common.registry.Registry;
import eva.common.util.NetUtil;
import eva.core.base.Pool;
import eva.core.base.config.ClientConfig;
import eva.core.exception.EvaClientException;
import eva.core.listener.NodeListener;
import eva.core.transport.codec.kryo.KryoCodecUtil;
import eva.core.transport.codec.kryo.KryoDecoder;
import eva.core.transport.codec.kryo.KryoEncoder;
import eva.core.transport.codec.kryo.KryoPoolFactory;
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
import io.netty.util.AttributeKey;

class ClientProvider implements Pool<ClientWrapper, InetSocketAddress> {

	private static final Logger LOG = LoggerFactory.getLogger(ClientProvider.class);

	public static final ClientProvider get() {
		return ClientProviderHolder.INSTANCE;
	}

	private static final class ClientProviderHolder {
		private static final ClientProvider INSTANCE = new ClientProvider();
	}

	static final AttributeKey<String> CHANNEL_ADDR_KEY = AttributeKey.valueOf("targetAddress");
	static final AttributeKey<UUID> CHANNEL_ID = AttributeKey.valueOf("channelId");

	// key: address, value: channels
	volatile Map<String, LinkedBlockingQueue<ClientWrapper>> POOL = Maps.newConcurrentMap();

	// private volatile Map<String, Set<String>> INTERFACE_HOSTS;

	private volatile Map<InetSocketAddress, Bootstrap> ADDRESS_BOOTSTRAP = Maps.newConcurrentMap();

	private ReentrantLock lock = new ReentrantLock();

	private KryoCodecUtil kryoCodecUtil = new KryoCodecUtil(KryoPoolFactory.getKryoPoolInstance());

	private volatile boolean isSingleHost;

	private String serverAddress;

	private String localHostIP;

	private ClientConfig config;

	private ClientProvider() {
		try {
			localHostIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			LOG.error(e.getMessage());
		}
	}

	synchronized boolean prepare() throws InterruptedException, KeeperException, IOException {
		clear();
		if (!isSingleHost) {
			Registry reg = Registry.get();
			reg.addObserver(new NodeListener() {
				@Override
				public void onAdd(Observable o, NodeChangeEvent event) {
				}

				@Override
				public void onDelete(Observable o, NodeChangeEvent event) {

				}
			});
			ZooKeeper zk = new ZooKeeper(serverAddress, 30 * 1000, reg);
			reg.syncData(zk);
			// Map<String, Set<String>> INTERFACE_HOSTS =
			// Registry.REGISTRY_DATA;
			if (Objects.nonNull(Registry.REGISTRY_DATA) && !Registry.REGISTRY_DATA.isEmpty()) {
				Set<Entry<String, Set<String>>> interfaceHostSet = Registry.REGISTRY_DATA.entrySet();
				Set<String> addresses = Sets.newHashSet();
				for (Entry<String, Set<String>> en : interfaceHostSet) {
					addresses.addAll(en.getValue());
				}
				for (String addr : addresses) {
					createAddressPool(addr);
				}
			}
		} else {
			String addr = serverAddress;
			createAddressPool(addr);
		}
		Thread.sleep(3 * 1000L);
		if (isSingleHost) {
			return POOL.get(serverAddress).size() > 0;
		}
		return true;
	}

	private synchronized void createAddressPool(String addr) {
		LinkedBlockingQueue<ClientWrapper> llist = POOL.get(addr);
		if (Objects.isNull(llist)) {
			llist = new LinkedBlockingQueue<>(config.getCoreSizePerHost());
			POOL.put(addr, llist);
		}
		ClientWrapper wrapper = null;
		for (int i = 0; i < config.getCoreSizePerHost(); i++) {
			try {
				wrapper = create(NetUtil.getAddress(addr));
				if (Objects.nonNull(wrapper)) {
					POOL.get(addr).offer(wrapper);
				} else {
					LOG.error("Cannot create eva client for target server: " + addr);
					break;
				}
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
		}
	}

	@Override
	public void clear() {
		try {
			lock.lock();
			POOL.clear();
		} finally {
			lock.unlock();
		}

	}

	@Override
	public ClientWrapper create(InetSocketAddress address) throws Exception {
		boolean status = NetUtil.pingHost(address.getAddress().getHostAddress(), address.getPort());
		if (!status) {
			return null;
		}
		ClientWrapper wrap = null;
		Bootstrap existing = ADDRESS_BOOTSTRAP.get(address);
		if (Objects.isNull(existing)) {
			final Bootstrap bootstrap = new Bootstrap();
			EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
			bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
					.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE).option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline pipeline = ch.pipeline();
							// pipeline.addLast(new IdleStateHandler(0, 15, 0));
							pipeline.addLast(new KryoEncoder(kryoCodecUtil));
							pipeline.addLast(new KryoDecoder(kryoCodecUtil));
							pipeline.addLast(new EvaClientHandler());
						}
					});
			ADDRESS_BOOTSTRAP.put(address, bootstrap);
		}
		Bootstrap bootstrap = ADDRESS_BOOTSTRAP.get(address);
		ChannelFuture channelFuture = bootstrap.connect(address.getAddress().getHostAddress(), address.getPort());
		Channel channel = null;
		channel = channelFuture.sync().channel();
		if (channel.isActive() && channel.isOpen() && channel.isWritable()) {
			channel.attr(CHANNEL_ADDR_KEY).set(address.getAddress().getHostAddress() + ":" + address.getPort());
			UUID channelId = UUID.randomUUID();
			channel.attr(CHANNEL_ID).set(channelId);
			wrap = new ClientWrapper(channel, channelId,
					address.getAddress().getHostAddress() + ":" + address.getPort());
		} else {
			throw new Exception("Cannot create available channel for target server: " + address);
		}
		return wrap;
	}

	private String getChannelAddress(Collection<String> addresses) throws EvaClientException {
		if (isSingleHost) {
			return serverAddress;
		}
		return BalanceStrategyFactory.getApplicableAddress(addresses, BalanceStrategyFactory.getStrategy(config),
				localHostIP);
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

	public void setConfig(ClientConfig config) {
		this.config = config;
	}

	public long getGlobalTimeoutMillSec() {
		return config.getGlobalTimoutMilliSec();
	}

	@Override
	public void removeSource(ClientWrapper target) {
		String addr = target.getTargetAddress();
		LinkedBlockingQueue<ClientWrapper> channels = POOL.get(addr);
		if (Objects.nonNull(channels)) {
			channels.remove(target);
			target.getChannel().close();
		}
	}

	@Override
	public ClientWrapper getSource(Class<?> serviceClass) throws Exception {
		String address = null;
		if (isSingleHost) {
			address = getChannelAddress(null);
		} else {
			Set<String> serviceAddresses = Registry.REGISTRY_DATA.get(serviceClass.getName());
			address = getChannelAddress(serviceAddresses);
		}
		LinkedBlockingQueue<ClientWrapper> llist = POOL.get(address);
		if (Objects.isNull(llist)) {
			createAddressPool(address);
		}
		ClientWrapper wrap = llist.poll(300, TimeUnit.MILLISECONDS);
		if (Objects.isNull(wrap)
				|| (!wrap.getChannel().isActive() || !wrap.getChannel().isOpen() || !wrap.getChannel().isWritable())) {
			if (Objects.nonNull(wrap)) {
				wrap.getChannel().flush().close();
				wrap = create(NetUtil.getAddress(address));
			} else {
				wrap = tryGet(address);
			}
		}
		return wrap;
	}

	private ClientWrapper tryGet(String address) throws Exception {
		ClientWrapper wrapper = create(NetUtil.getAddress(address));
		wrapper.setNeedRecycle(false);
		return wrapper;
	}

	@Override
	public void putback(ClientWrapper source) throws Exception {
		String addr = source.getTargetAddress();
		LinkedBlockingQueue<ClientWrapper> channels = POOL.get(addr);
		channels = POOL.get(addr);
		boolean flag = false;
		if (source.getChannel().isActive() && source.getChannel().isOpen() && source.getChannel().isWritable()) {
			flag = channels.offer(source);
		} else {
			source.getChannel().flush().close();
			source = create(NetUtil.getAddress(source.getTargetAddress()));
			flag = channels.offer(source);
		}
		if (!flag) {
			LOG.info("Cannot offer channel to POOL[" + source.getTargetAddress() + "], the POOL might be full.");
		}
	}

	// ClientWrapper createIfNecessary(InetSocketAddress address) throws
	// Exception {
	// LinkedBlockingQueue<ClientWrapper> channels =
	// POOL.get(NetUtil.getAddress(address));
	// if (channels.size() < config.getMaxSizePerHost()) {
	// ClientWrapper wrapper = create(address);
	// if (Objects.nonNull(wrapper)) {
	// channels.offer(wrapper);
	// return wrapper;
	// }
	// }
	// return null;
	// }

}
