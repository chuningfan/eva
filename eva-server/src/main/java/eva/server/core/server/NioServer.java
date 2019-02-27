package eva.server.core.server;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.core.base.BaseServer;
import eva.core.base.config.ServerConfig;
import eva.core.dto.ProviderMetadata;
import eva.core.dto.StatusEvent;
import eva.core.transport.codec.kryo.KryoCodecUtil;
import eva.core.transport.codec.kryo.KryoDecoder;
import eva.core.transport.codec.kryo.KryoEncoder;
import eva.core.transport.codec.kryo.KryoPoolFactory;
import eva.server.core.async.Processor;
import eva.server.core.handler.NioServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericFutureListener;

public class NioServer extends BaseServer {

	private static final Logger LOG = LoggerFactory.getLogger(NioServer.class);

	private EventLoopGroup bossGroup;

	private EventLoopGroup workerGroup;

	private KryoCodecUtil kryoCodecUtil = new KryoCodecUtil(KryoPoolFactory.getKryoPoolInstance());
	
	public static int QUEUE_CAPACITY = 100;

	public NioServer(ServerConfig config, ProviderMetadata providerMetadata) {
		super(config);
		providerMetadata.setHost(host);
		providerMetadata.setPort(config.getPort());
		providerMetadata.setProviderName(config.getServerId());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void init(ServerConfig config) {
		if (config.isAsyncProcessing()) {
			QUEUE_CAPACITY = config.getAsyncQueueSize();
		}
		int bossSize = config.getBossSize();
		int workerSize = config.getWorkerSize();
		
		bossGroup = new NioEventLoopGroup(bossSize);
		workerGroup = new NioEventLoopGroup(workerSize);
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup);
		b.channel(NioServerSocketChannel.class);
		b.option(ChannelOption.SO_BACKLOG, 1024);
		b.option(ChannelOption.SO_RCVBUF, 10*1024*1024);
		b.option(ChannelOption.SO_SNDBUF, 1024*1024);
		b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		b.childOption(ChannelOption.TCP_NODELAY, true);
		b.childOption(ChannelOption.SO_KEEPALIVE, true);
        b.childHandler(new ServerChannelInitializer());
        if (config.isAsyncProcessing()) {
        	Processor.getInstance().init();
        }
		ChannelFuture f;
		try {
			f = b.bind(config.getPort()).sync();
			notifyObservers(StatusEvent.getStartupEvent());
			LOG.info("******** Eva is ready! Hello, World! ********");
			f.channel().closeFuture().sync().addListener((GenericFutureListener<? extends Future<? super Void>>) new FutureListener<ChannelFuture>() {
				@Override
				public void operationComplete(Future<ChannelFuture> paramF) throws Exception {
					notifyObservers(StatusEvent.getCloseEvent());
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
			notifyObservers(StatusEvent.getFailedEvent(e));
		} finally {
			stopGracefully();
		}
	}

	@SuppressWarnings("unchecked")
	protected ChannelHandler getDecoder() {
		return new KryoDecoder(kryoCodecUtil);
	}
	
	@SuppressWarnings("unchecked")
	protected ChannelHandler getEncoder() {
		return new KryoEncoder(kryoCodecUtil);
	}
	
	private class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
			pipeline.addLast(new IdleStateHandler(15, 0, 0, TimeUnit.SECONDS));
			pipeline.addLast("decoder", getDecoder());
	        pipeline.addLast("encoder", getEncoder());
	        pipeline.addLast("handler", new NioServerHandler(config));
	        if (config.getServerTimeoutSec() > 0) {
	        	pipeline.addLast(new ReadTimeoutHandler(config.getServerTimeoutSec()));
	        }
		}
	}

}
