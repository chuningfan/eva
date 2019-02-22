package eva.common.transport.codec.kryo;

import eva.common.transport.codec.MessageCodecUtil;
import eva.common.transport.codec.MessageDecoder;

public class KryoDecoder extends MessageDecoder {

    public KryoDecoder(MessageCodecUtil util) {
        super(util);
    }
}