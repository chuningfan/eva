package eva.common.transport.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import eva.common.transport.Body;
import eva.common.transport.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class NioServerEncoder extends MessageToByteEncoder<Packet> {

	@Override
	protected void encode(ChannelHandlerContext arg0, Packet p, ByteBuf arg2) throws Exception {
		long requestId = p.getRequestId();
		Body body = p.getBody();
		byte[] jsonBytes = JSON.toJSONBytes(body, SerializerFeature.BeanToArray);
		p.setBodySize(jsonBytes.length);
		arg2.writeLong(requestId);
		arg2.writeInt(p.getBodySize());
		arg2.writeBytes(jsonBytes);
	}

}
