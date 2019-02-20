package eva.common.transport.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import eva.common.transport.Body;
import eva.common.transport.Packet;
import eva.common.util.PacketUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class NioServerEncoder extends MessageToByteEncoder<Packet> {

	@Override
	protected void encode(ChannelHandlerContext arg0, Packet arg1, ByteBuf arg2) throws Exception {
		Packet p = new Packet();
		long requestId = p.getRequestId();
		Body body = arg1.getBody();
		p.setBody(body);
		PacketUtil.setBodySize(p);
		int bodySize = p.getBodySize();
		arg2.writeLong(requestId);
		arg2.writeInt(bodySize);
		byte[] jsonBytes = JSON.toJSONBytes(body, SerializerFeature.BeanToArray);
		arg2.writeBytes(jsonBytes);
	}

}
