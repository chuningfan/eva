package eva.server.core.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.common.base.BaseServer;
import eva.common.base.config.ServerConfig;
import eva.common.transport.codec.NioServerDecoder;
import eva.common.transport.codec.NioServerEncoder;
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

public class NioServer extends BaseServer {

	public NioServer(ServerConfig config) {
		super(config);
	}

	private static final Logger LOG = LoggerFactory.getLogger(NioServer.class);

	private EventLoopGroup bossGroup;

	private EventLoopGroup workerGroup;

	public static int QUEUE_CAPACITY = 30;

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
		b.childHandler(new ServerChannelInitializer());
		b.option(ChannelOption.SO_BACKLOG, 128);
		b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		b.childOption(ChannelOption.SO_KEEPALIVE, true);
        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        if (config.isAsyncProcessing()) {
        	Processor.getInstance().init();
        }
		ChannelFuture f;
		try {
			f = b.bind(config.getPort()).sync();
			LOG.info("");
			f.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			stopGracefully();
		}
	}

	@SuppressWarnings("unchecked")
	protected ChannelHandler getDecoder() {
		return new NioServerDecoder();
	}
	
	@SuppressWarnings("unchecked")
	protected ChannelHandler getEncoder() {
		return new NioServerEncoder();
	}
	
	private class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			pipeline.addLast("decoder", getDecoder());
	        pipeline.addLast("encoder", getEncoder());
	        pipeline.addLast("handler", new NioServerHandler(config));
		}
	}

}
