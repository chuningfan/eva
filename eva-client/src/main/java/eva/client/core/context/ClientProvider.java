package eva.client.core.context;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eva.balance.strategies.BalanceStrategyFactory;
import eva.balance.strategies.BalanceStrategyFactory.Strategy;
import eva.client.core.dto.ClientWrapper;
import eva.client.core.dto.EvaAddressChannelCollection;
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

class ClientProvider implements Pool<ClientWrapper, InetSocketAddress> {

	public static final ClientProvider get() {
		return ClientProviderHolder.INSTANCE;
	}
	
	private static final class ClientProviderHolder {
		private static final ClientProvider INSTANCE = new ClientProvider();
	}
	
	// key: address, value: channels
	private volatile Map<String, EvaAddressChannelCollection<ClientWrapper>> POOL = Maps.newConcurrentMap();

	private volatile Map<String, Set<String>> INTERFACE_HOSTS;

	private ReentrantLock lock = new ReentrantLock();
	
	private ScheduledExecutorService poolWatcher;

	private KryoCodecUtil kryoCodecUtil = new KryoCodecUtil(KryoPoolFactory.getKryoPoolInstance());

	private boolean isSingleHost;

	private String serverAddress;

	private Strategy balanceStrategy;

	private String localHostIP;

	private long globalTimeoutMillSec;
	
	private int coreSizePerHost;
	
	private int maxSizePerHost;
	
	private ClientProvider() {
		Set<String> addrSet = null;
		try {
			localHostIP = InetAddress.getLocalHost().getHostAddress();
			addrSet = POOL.keySet();
			if (Objects.isNull(addrSet) || addrSet.isEmpty()) {
				Thread.sleep(1 * 3000);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.poolWatcher = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				final Thread daemon = Executors.defaultThreadFactory().newThread(r);
				daemon.setDaemon(true);
				daemon.setName("Pool-Watcher");
				return daemon;
			}
		});
		if (Objects.nonNull(addrSet) && !addrSet.isEmpty()) {
			for (String addr: addrSet) {
				EvaAddressChannelCollection<ClientWrapper> evaAddrChannels = POOL.get(addr);
				if (Objects.nonNull(evaAddrChannels) && !evaAddrChannels.isEmpty()) {
					poolWatcher.scheduleAtFixedRate(new Runnable() {
						private volatile int previousSize = -1;
						private volatile int factor = 0;
						@Override
						public void run() {
							int currentSize = evaAddrChannels.getCurrentSize();
							int retryTime = 0;
							if (previousSize < 0) {
								do {
									previousSize = currentSize;
									retryTime++;
								} while (previousSize < 1 && retryTime < 3);
							} else {
								if (previousSize <= currentSize) {
									if (currentSize > coreSizePerHost) {
										factor ++;
										if (factor >= 3) {
											while (currentSize > coreSizePerHost) {
												evaAddrChannels.WatcherPoll();
											}
											factor = 0;
										}
									}
								} else {
									factor --;
								}
							}
						}
					}, 15, 10 * 1000, TimeUnit.MILLISECONDS);
				}
			}
		}
	}

	public void prepare() {
		clear();
		if (!isSingleHost) {
			INTERFACE_HOSTS = Registry.get().getAllNodes();
			if (Objects.nonNull(INTERFACE_HOSTS) && !INTERFACE_HOSTS.isEmpty()) {
				Set<Entry<String, Set<String>>> interfaceHostSet = INTERFACE_HOSTS.entrySet();
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
	}

	private void createAddressPool(String addr) {
		EvaAddressChannelCollection<ClientWrapper> llist = POOL.get(addr);
		if (Objects.isNull(llist)) {
			llist = new EvaAddressChannelCollection<ClientWrapper>(coreSizePerHost, maxSizePerHost, addr);
			POOL.put(addr, llist);
		}
		Executors.newSingleThreadExecutor().submit(() -> {
			for (int i = 0; i < coreSizePerHost; i ++) {
				try {
					POOL.get(addr).add(create(NetUtil.getAddress(addr)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
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
	public ClientWrapper create(InetSocketAddress address) throws EvaClientException {
		ClientWrapper wrap = null;
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
			wrap = new ClientWrapper(channel, address.getAddress().getHostAddress() + ":" + address.getPort());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return wrap;
	}

	private String getChannelAddress(Collection<String> addresses) throws EvaClientException {
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

	public Strategy getBalanceStrategy() {
		return balanceStrategy;
	}

	public void setBalanceStrategy(Strategy balanceStrategy) {
		this.balanceStrategy = balanceStrategy;
	}

	public long getGlobalTimeoutMillSec() {
		return globalTimeoutMillSec;
	}

	public void setGlobalTimeoutMillSec(long globalTimeoutMillSec) {
		this.globalTimeoutMillSec = globalTimeoutMillSec;
	}

	public int getCoreSizePerHost() {
		return coreSizePerHost;
	}

	public void setCoreSizePerHost(int coreSizePerHost) {
		this.coreSizePerHost = coreSizePerHost;
	}

	public int getMaxSizePerHost() {
		return maxSizePerHost;
	}

	public void setMaxSizePerHost(int maxSizePerHost) {
		this.maxSizePerHost = maxSizePerHost;
	}

	@Override
	public void removeSource(ClientWrapper target) {
		String addr = target.getTargetAddress();
		LinkedList<ClientWrapper> channels = POOL.get(addr);
		if (Objects.nonNull(channels)) {
			channels.remove(target);
			target.getChannel().close();
		}
	}

	@Override
	public ClientWrapper getSource(Class<?> serviceClass) throws EvaClientException {
		String address = null;
		if (isSingleHost) {
			address = getChannelAddress(null);
		} else {
			Set<String> serviceAddresses = INTERFACE_HOSTS.get(serviceClass);
			address = getChannelAddress(serviceAddresses);
		}
		EvaAddressChannelCollection<ClientWrapper> llist = POOL.get(address);
		if (Objects.isNull(llist)) {
			createAddressPool(address);
			return POOL.get(address).poll();
		} else {
			ClientWrapper wrap = llist.poll();
			return wrap;
		}
	}

	@Override
	public void putback(ClientWrapper source) {
		String addr = source.getTargetAddress();
		LinkedList<ClientWrapper> channels = POOL.get(addr);
		channels = POOL.get(addr);
		channels.offer(source);
	}

}
