package eva.server.core.handler;

import org.springframework.context.ApplicationContext;

import eva.core.base.config.ServerConfig;
import eva.core.transport.Packet;
import eva.core.valve.EvaPipeline;
import eva.core.valve.EvaPipeline.Direction;
import eva.core.valve.Result;
import eva.server.core.context.AncientContext;
import eva.server.core.valve.Completed;
import eva.server.core.valve.Invoker;
import eva.server.core.valve.PacketChecker;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@io.netty.channel.ChannelHandler.Sharable
public class NioServerHandler extends SimpleChannelInboundHandler<Packet> {

	private static final ApplicationContext CONTEXT = AncientContext.CONTEXT;

	private ServerConfig config;

	private static EvaPipeline<ServerParamWrapper, Result> PIPELINE = null;
	
	static {
		PIPELINE = new EvaPipeline<ServerParamWrapper, Result>();
		PIPELINE.addLast(new PacketChecker()).addLast(new Invoker()).addLast(new Completed());
	}

	public NioServerHandler(ServerConfig config) {
		this.config = config;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		ServerParamWrapper wrapper = new ServerParamWrapper(CONTEXT, config, packet, ctx);
		Result res = PIPELINE.doProcess(Direction.FORWARD, wrapper, Result.getDefault(null));
		if (!res.isSuccessful()) {
			throw res.getException();
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.flush();
		ctx.close();
		throw new Exception(cause);
	}

}
