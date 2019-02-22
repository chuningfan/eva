package eva.client.core.handler;

import eva.core.transport.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class EvaClientHandler extends SimpleChannelInboundHandler<Response> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {
		
	}
	
}
