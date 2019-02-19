package eva.common.transport.codec;

import eva.common.transport.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class NioServerEncoder extends MessageToByteEncoder<Packet> {

	@Override
	protected void encode(ChannelHandlerContext arg0, Packet arg1, ByteBuf arg2) throws Exception {
		
	}

}
