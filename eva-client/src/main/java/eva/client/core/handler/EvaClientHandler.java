package eva.client.core.handler;

import java.util.Objects;

import eva.client.core.context.RPCClient;
import eva.client.core.context.RPCClient.ResponseFuture;
import eva.core.transport.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class EvaClientHandler extends SimpleChannelInboundHandler<Response> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {
		long requestId = response.getRequestId();
		ResponseFuture<Response> future = RPCClient.getFuture(requestId);
		if (Objects.nonNull(future)) {
			future.setResult(response);
		} else {
			throw new Exception("Cannot find <response future> for request ID:" + requestId);
		}
	}
	
}
