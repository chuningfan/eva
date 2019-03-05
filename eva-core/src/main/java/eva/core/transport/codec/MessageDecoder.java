package eva.core.transport.codec;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {

	private static final Logger LOG = Logger.getLogger("MessageDecoder");
	
	final public static int MESSAGE_LENGTH = MessageCodecUtil.MESSAGE_LENGTH;
    private MessageCodecUtil util = null;

    public MessageDecoder(final MessageCodecUtil util) {
        this.util = util;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < MESSAGE_LENGTH) {
            return;
        }
        in.markReaderIndex();
        int messageLength = in.readInt();
//        if (messageLength < 0) {
//            ctx.close();
//        }
        if (in.readableBytes() < messageLength) {
            in.resetReaderIndex();
            return;
        } else {
            byte[] messageBody = new byte[messageLength];
            in.readBytes(messageBody);
            try {
                Object obj = util.decode(messageBody);
                out.add(obj);
            } catch (IOException e) {
            	LOG.warning(e.getMessage());
            }
        }
    }

}
