package eva.common.transport.codec;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;

import eva.common.transport.Body;
import eva.common.transport.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class NioServerDecoder extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext arg0, ByteBuf arg1, List<Object> arg2) throws Exception {
		Packet p = new Packet();
		long requestId = arg1.readLong();
		int bodySize = arg1.readInt();
		ByteBuf buff = arg1.readBytes(bodySize);
		if (buff.hasArray()) {
			Body b = JSON.parseObject(buff.array(), Body.class, Feature.SupportAutoType, Feature.SupportArrayToBean, Feature.SupportNonPublicField, Feature.UseObjectArray, Feature.UseBigDecimal);
			p.setBodySize(bodySize);
			p.setBody(b);
		}
		p.setRequestId(requestId);
		arg2.add(p);
	}

}
