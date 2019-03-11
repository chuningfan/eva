package eva.core.transport.codec.kryo;

import eva.core.transport.codec.MessageCodecUtil;
import eva.core.transport.codec.MessageDecoder;

public class KryoDecoder extends MessageDecoder {

    public KryoDecoder(MessageCodecUtil util) {
        super(util);
    }
}