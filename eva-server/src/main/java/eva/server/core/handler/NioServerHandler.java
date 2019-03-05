package eva.server.core.handler;

import eva.core.base.config.ServerConfig;
import eva.core.transport.Packet;
import eva.core.valve.EvaPipeline;
import eva.core.valve.EvaPipeline.Direction;
import eva.core.valve.Result;
import eva.server.core.valve.Completed;
import eva.server.core.valve.PacketChecker;
import eva.server.core.valve.invoker.Invoker;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;

@io.netty.channel.ChannelHandler.Sharable
public class NioServerHandler extends SimpleChannelInboundHandler<Packet> {

	private ServerConfig config;

	private static EvaPipeline<ServerParamWrapper, Result> PIPELINE = null;
	
	private static final AttributeKey<Integer> WRITER_KEY = AttributeKey.valueOf("writeFailedTimes");
	private static final AttributeKey<Integer> READER_KEY = AttributeKey.valueOf("readFailedTimes");
	private static final AttributeKey<Integer> ALL_KEY = AttributeKey.valueOf("allFailedTimes");
	
	public NioServerHandler(ServerConfig config) {
		this.config = config;
		PIPELINE = new EvaPipeline<ServerParamWrapper, Result>();
		PIPELINE.addLast(new PacketChecker()).addLast(new Invoker(config.getProvider())).addLast(new Completed());
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		resetChannelState(ctx);
		ServerParamWrapper wrapper = new ServerParamWrapper(config, packet, ctx);
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

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelActive();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			switch (event.state()) {
			case WRITER_IDLE:
//				if (ctx.channel().hasAttr(WRITER_KEY)) {
//					Integer times = ctx.channel().attr(WRITER_KEY).get();
//					if ((times + 1) > 2) {
//						ctx.channel().flush().close();
//					} else {
//						ctx.channel().attr(WRITER_KEY).set(times + 1);
//					}
//				} else {
//					ctx.channel().attr(WRITER_KEY).set(1);
//				}
				break;
			case READER_IDLE:
				if (ctx.channel().hasAttr(READER_KEY)) {
					Integer times = ctx.channel().attr(READER_KEY).get();
					if ((times + 1) > 2) {
						ctx.channel().flush().close();
					} else {
						ctx.channel().attr(READER_KEY).set(times + 1);
					}
				} else {
					ctx.channel().attr(READER_KEY).set(1);
				}
				break;
			case ALL_IDLE:
//				if (ctx.channel().hasAttr(ALL_KEY)) {
//					Integer times = ctx.channel().attr(ALL_KEY).get();
//					if ((times + 1) > 2) {
//						ctx.channel().flush().close();
//					} else {
//						ctx.channel().attr(ALL_KEY).set(times + 1);
//					}
//				} else {
//					ctx.channel().attr(ALL_KEY).set(1);
//				}
				break;
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}
	
	private void resetChannelState(ChannelHandlerContext ctx) {
		if (ctx.channel().hasAttr(READER_KEY)) {
			ctx.channel().attr(READER_KEY).set(0);
		}
		if (ctx.channel().hasAttr(WRITER_KEY)) {
			ctx.channel().attr(WRITER_KEY).set(0);
		}
		if (ctx.channel().hasAttr(ALL_KEY)) {
			ctx.channel().attr(ALL_KEY).set(0);
		}
	}
	
}
