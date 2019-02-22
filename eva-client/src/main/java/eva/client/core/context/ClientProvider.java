package eva.client.core.context;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eva.balance.strategies.BalanceStrategyFactory;
import eva.balance.strategies.BalanceStrategyFactory.Strategy;
import eva.client.core.dto.ClientWrap;
import eva.client.core.handler.EvaClientHandler;
import eva.common.util.CommonUtil;
import eva.common.util.NetUtil;
import eva.core.base.Pool;
import eva.core.exception.EvaClientException;
import eva.core.registry.Registry;
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

class ClientProvider implements Pool<ClientWrap, InetSocketAddress> {

	// key: address, value: channels
	private volatile Map<String, LinkedList<ClientWrap>> POOL = Maps.newConcurrentMap();

	private volatile Map<String, Set<String>> INTERFACE_HOSTS;

	private ReentrantLock lock = new ReentrantLock();

	private volatile Map<String, ReentrantLock> addrLockMap = Maps.newConcurrentMap();

	private KryoCodecUtil kryoCodecUtil = new KryoCodecUtil(KryoPoolFactory.getKryoPoolInstance());

	private static final class ClientProviderHolder {
		private static final ClientProvider INSTANCE = new ClientProvider();
	}

	public static final ClientProvider get() {
		return ClientProviderHolder.INSTANCE;
	}

	private boolean isSingleHost;

	private String serverAddress;

	private int maxSizePerHost;

	private Strategy balanceStrategy;

	private String localHostIP;

	private ClientProvider() {
		try {
			localHostIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void prepare() {
		if (!isSingleHost) {
			INTERFACE_HOSTS = Registry.get().getAllNodes();
			if (Objects.nonNull(INTERFACE_HOSTS) && !INTERFACE_HOSTS.isEmpty()) {
				Set<Entry<String, Set<String>>> interfaceHostSet = INTERFACE_HOSTS.entrySet();
				Set<String> addresses = Sets.newHashSet();
				for (Entry<String, Set<String>> en : interfaceHostSet) {
					addresses.addAll(en.getValue());
				}
				for (String addr : addresses) {
					addrLockMap.put(addr, new ReentrantLock());
					String host = NetUtil.getHost(addr);
					int port = NetUtil.getPort(addr);
					LinkedList<ClientWrap> llist = POOL.get(addr);
					if (Objects.isNull(llist)) {
						llist = Lists.newLinkedList();
						POOL.put(addr, llist);
					}
					int failedTime = 0;
					for (; llist.size() < maxSizePerHost && failedTime < 3;) {
						ClientWrap wrap;
						try {
							wrap = create(new InetSocketAddress(host, port));
							if (Objects.nonNull(wrap)) {
								llist.offer(wrap);
							} else {
								failedTime++;
							}
						} catch (EvaClientException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} else {
			String addr = serverAddress;
			addrLockMap.put(addr, new ReentrantLock());
			String[] arr = addr.split(":");
			String host = arr[0];
			int port = Integer.parseInt(arr[1]);
			LinkedList<ClientWrap> llist = POOL.get(addr);
			if (Objects.isNull(llist)) {
				llist = Lists.newLinkedList();
				POOL.put(addr, llist);
			}
			int failedTime = 0;
			for (; llist.size() < maxSizePerHost && failedTime < 3;) {
				ClientWrap wrap;
				try {
					wrap = create(new InetSocketAddress(host, port));
					if (Objects.nonNull(wrap)) {
						llist.offer(wrap);
					} else {
						failedTime++;
					}
				} catch (EvaClientException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void clear() {
		try {
			lock.lock();
			Map<String, LinkedList<ClientWrap>> temp = CommonUtil.deepCopy(POOL);
			if (Objects.nonNull(temp)) {
				Set<Entry<String, LinkedList<ClientWrap>>> entries = temp.entrySet();
				for (Entry<String, LinkedList<ClientWrap>> e: entries) {
					if (Objects.nonNull(e.getValue())) {
						e.getValue().stream().forEach(c -> {
							c.getChannel().close();
						});
					}
				}
				temp.clear();
				POOL = temp;
			}
		} finally {
			lock.unlock();
		}

	}

	@Override
	public ClientWrap create(InetSocketAddress address) throws EvaClientException {
		ClientWrap wrap = null;
		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
				.option(ChannelOption.TCP_NODELAY, Boolean.TRUE).handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast(new KryoEncoder(kryoCodecUtil));
						pipeline.addLast(new KryoDecoder(kryoCodecUtil));
						pipeline.addLast(new EvaClientHandler());
					}
				});
		ChannelFuture channelFuture = bootstrap.connect(address.getAddress().getHostAddress(), address.getPort());
		Channel channel = null;
		try {
			channel = channelFuture.sync().channel();
			wrap = new ClientWrap(channel, address.getAddress().getHostAddress() + ":" + address.getPort());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return wrap;
	}

	private String getChannelAddress(Set<String> addresses) throws EvaClientException {
		if (isSingleHost) {
			return serverAddress;
		}
		return BalanceStrategyFactory.getApplicableAddress(addresses, balanceStrategy, localHostIP);
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

	public int getMaxSizePerHost() {
		return maxSizePerHost;
	}

	public void setMaxSizePerHost(int maxSizePerHost) {
		this.maxSizePerHost = maxSizePerHost;
	}

	public Strategy getBalanceStrategy() {
		return balanceStrategy;
	}

	public void setBalanceStrategy(Strategy balanceStrategy) {
		this.balanceStrategy = balanceStrategy;
	}

	@Override
	public void removeSource(ClientWrap target) {
		String addr = target.getTargetAddress();
		ReentrantLock addrLock = addrLockMap.get(addr);
		try {
			addrLock.lock();
			LinkedList<ClientWrap> channels = POOL.get(addr);
			if (Objects.nonNull(channels)) {
				channels.remove(target);
				target.getChannel().close();
			}
		} finally {
			addrLock.unlock();
		}
	}

	@Override
	public ClientWrap getSource(Class<?> serviceClass) throws EvaClientException {
		String address = null;
		if (isSingleHost) {
			address = getChannelAddress(null);
		} else {
			Set<String> serviceAddresses = INTERFACE_HOSTS.get(serviceClass);
			address = getChannelAddress(serviceAddresses);
		}
		LinkedList<ClientWrap> llist = POOL.get(address);
		ReentrantLock addrLock = addrLockMap.get(address);
		try {
			addrLock.lock();
			if (Objects.isNull(llist)) {
				POOL.put(address, Lists.newLinkedList());
				return create(NetUtil.getAddress(address));
			} else {

				if (llist.isEmpty()) {
					int retryTime = 0;
					while (retryTime++ < 3) {
						try {
							Thread.sleep(300L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (llist.stream().findAny().isPresent()) {
							return llist.poll();
						}
					}
					return create(NetUtil.getAddress(address));
				} else {
					if (llist.stream().findAny().isPresent()) {
						return llist.poll();
					}
				}
			}
		} finally {
			addrLock.unlock();
		}
		return null;
	}

	@Override
	public void putback(ClientWrap source) {
		String addr = source.getTargetAddress();
		ReentrantLock addrLock = addrLockMap.get(addr);
		try {
			addrLock.lock();
			LinkedList<ClientWrap> channels = POOL.get(addr);
			if (Objects.isNull(channels)) {
				POOL.put(addr, Lists.newLinkedList());
			}
			channels = POOL.get(addr);
			if (channels.size() < maxSizePerHost) {
				channels.offer(source);
			}
		} finally {
			addrLock.unlock();
		}
	}

}
