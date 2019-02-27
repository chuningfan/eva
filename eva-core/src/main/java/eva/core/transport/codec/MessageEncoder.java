package eva.core.transport.codec;

import java.io.IOException;
import java.util.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class MessageEncoder extends MessageToByteEncoder<Object> {
	
	private static final Logger LOG = Logger.getLogger("MessageEncoder");
	
	private MessageCodecUtil util = null;

    public MessageEncoder(final MessageCodecUtil util) {
        this.util = util;
    }

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
		try {
			util.encode(out, msg);
		} catch (IOException e) {
			LOG.warning(e.getMessage());
		}
	}

}
