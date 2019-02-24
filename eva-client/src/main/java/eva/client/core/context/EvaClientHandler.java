package eva.client.core.context;

import java.util.Objects;

import com.esotericsoftware.minlog.Log;

import eva.client.core.context.Eva.ResponseFuture;
import eva.core.transport.Response;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
public class EvaClientHandler extends SimpleChannelInboundHandler<Response> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {
		long requestId = response.getRequestId();
		Log.info("Client received response, request ID: " + requestId);
		ResponseFuture<Response> future = Eva.getFuture(requestId);
		if (Objects.nonNull(future)) {
			future.setResult(response);
		} else {
//			throw new Exception("Cannot find <response future> for request ID:" + requestId);
			Log.error("Cannot find <response future> for request ID:" + requestId);
		}
	}
	
}
