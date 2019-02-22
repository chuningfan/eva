package eva.core.transport.codec;

import java.io.IOException;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {

//	@Override
//	protected void decode(ChannelHandlerContext arg0, ByteBuf arg1, List<Object> arg2) throws Exception {
//		Packet p = new Packet();
//		long requestId = arg1.readLong();
//		int bodySize = arg1.readInt();
//		byte[] bytes = new byte[bodySize];
//		arg1.readBytes(bytes);
//		Body b = (Body) JSON.parse(bytes);
//		p.setBodySize(bodySize);
//		p.setBody(b);
//		p.setRequestId(requestId);
//		arg2.add(p);
//	}
	
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
        if (messageLength < 0) {
            ctx.close();
        }
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
            	e.printStackTrace();
            }
        }
    }

}
