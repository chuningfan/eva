package eva.core.transport.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class MessageEncoder extends MessageToByteEncoder<Object> {

//	@Override
//	protected void encode(ChannelHandlerContext arg0, Packet p, ByteBuf arg2) throws Exception {
//		long requestId = p.getRequestId();
//		Body body = p.getBody();
//		byte[] jsonBytes = JSON.toJSONBytes(body);
//		p.setBodySize(jsonBytes.length);
//		arg2.writeLong(requestId);
//		arg2.writeInt(p.getBodySize());
//		arg2.writeBytes(jsonBytes);
//	}
	
	private MessageCodecUtil util = null;

    public MessageEncoder(final MessageCodecUtil util) {
        this.util = util;
    }

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		util.encode(out, msg);
	}

}
