package eva.core.transport.codec.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.esotericsoftware.kryo.pool.KryoPool;

import eva.common.util.CommonUtil;
import eva.core.transport.codec.MessageCodecUtil;
import io.netty.buffer.ByteBuf;

public class KryoCodecUtil implements MessageCodecUtil {

    private KryoPool pool;

    public KryoCodecUtil(KryoPool pool) {
        this.pool = pool;
    }

    public void encode(final ByteBuf out, final Object message) throws IOException {
    	ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            KryoSerialize kryoSerialization = new KryoSerialize(pool);
            kryoSerialization.serialize(byteArrayOutputStream, message);
            byte[] body = byteArrayOutputStream.toByteArray();
            int dataLength = body.length;
            out.writeInt(dataLength);
            out.writeBytes(body);
        } finally {
        	CommonUtil.closeStreams(byteArrayOutputStream);
        }
    }

    public Object decode(byte[] body) throws IOException {
    	ByteArrayInputStream byteArrayInputStream = null;
        try {
            byteArrayInputStream = new ByteArrayInputStream(body);
            KryoSerialize kryoSerialization = new KryoSerialize(pool);
            Object obj = kryoSerialization.deserialize(byteArrayInputStream);
            return obj;
        } finally {
        	CommonUtil.closeStreams(byteArrayInputStream);
        }
    }
}