package eva.common.transport.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface RpcSerialize {
	
	void serialize(OutputStream output, Object object) throws IOException;

    Object deserialize(InputStream input) throws IOException;
	
}
