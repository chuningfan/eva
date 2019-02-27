package eva.client.core.context;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import com.esotericsoftware.minlog.Log;

import eva.client.core.context.Eva.ResponseFuture;
import eva.client.core.dto.ClientWrapper;
import eva.core.transport.Response;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;

@Sharable
public class EvaClientHandler extends SimpleChannelInboundHandler<Response> {

	private static final AttributeKey<Integer> WRITER_KEY = AttributeKey.valueOf("writeFailedTimes");
	private static final AttributeKey<Integer> READER_KEY = AttributeKey.valueOf("readFailedTimes");
	private static final AttributeKey<Integer> ALL_KEY = AttributeKey.valueOf("allFailedTimes");

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {
		long requestId = response.getRequestId();
		resetChannelState(ctx);
		Log.info("Client received response, request ID: " + requestId);
		ResponseFuture<Response> future = Eva.getFuture(requestId);
		if (Objects.nonNull(future)) {
			future.setResult(response);
		} else {
			// throw new Exception("Cannot find <response future> for request
			// ID:" + requestId);
			Log.error("Cannot find <response future> for request ID:" + requestId);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			switch (event.state()) {
			case WRITER_IDLE:
				if (ctx.channel().hasAttr(WRITER_KEY)) {
					Integer times = ctx.channel().attr(WRITER_KEY).get();
					if ((times + 1) > 2) {
						ctx.channel().flush().close();
						String targetAddress = ctx.channel().attr(ClientProvider.CHANNEL_ADDR_KEY).get();
						UUID channelId = ctx.channel().attr(ClientProvider.CHANNEL_ID).get();
						LinkedBlockingQueue<ClientWrapper> list = ClientProvider.get().POOL.get(targetAddress);
						ClientWrapper wrapper = list.stream().anyMatch(cw -> cw.getChannel() == ctx.channel()) ? list.stream().filter(cw -> cw.getChannel() == ctx.channel()).findFirst().get() : null;
						if (Objects.isNull(wrapper)) {
							list.remove(wrapper);
						}
					} else {
						ctx.channel().attr(WRITER_KEY).set(times + 1);
					}
				} else {
					ctx.channel().attr(WRITER_KEY).set(1);
				}
				break;
			case READER_IDLE:
//				if (ctx.channel().hasAttr(READER_KEY)) {
//					Integer times = ctx.channel().attr(READER_KEY).get();
//					if ((times + 1) > 2) {
//						ctx.channel().flush().close();
//						
//					} else {
//						ctx.channel().attr(READER_KEY).set(times + 1);
//					}
//				} else {
//					ctx.channel().attr(READER_KEY).set(1);
//				}
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
