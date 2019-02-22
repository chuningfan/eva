package eva.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

public class CommonUtil {
	
	@SuppressWarnings("unchecked")
	public static final <T> T deepCopy(T target) {
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		ByteArrayInputStream bais = null;
		ObjectInputStream ois = null;
		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(target);
			bais = new ByteArrayInputStream(baos.toByteArray());
			ois = new ObjectInputStream(bais);
			return (T) ois.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				closeStreams(oos, baos, ois, bais);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static final void closeStreams(Closeable...streams) throws IOException {
		for (Closeable stream: streams) {
			if (Objects.nonNull(stream)) {
				stream.close();
			}
		}
	}
	
}
