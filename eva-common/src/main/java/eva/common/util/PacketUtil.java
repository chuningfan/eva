package eva.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

import com.alibaba.fastjson.JSON;

import eva.common.transport.Body;
import eva.common.transport.Packet;

public class PacketUtil {
	
	/**
	 * except request ID, body size.
	 * @param p
	 */
//	public static final void setBodySize(Packet p) {
//		Body body = p.getBody();
//		String json = JSON.toJSONString(body);
//		p.setBodySize(json.getBytes().length);
//	}
	
	public static Class<?>[] getTypes(Object...args) {
		if (Objects.isNull(args) || args.length == 0) {
			return null;
		}
		Class<?>[] types = new Class<?>[args.length];
		for (int i = 0; i < args.length; i ++) {
			types[i] = args[i].getClass();
		}
		return types;
	}
	
	public static byte[] ObjectToByte(Object obj) {
	    byte[] bytes = null;
	    try {
	        // object to bytearray
	        ByteArrayOutputStream bo = new ByteArrayOutputStream();
	        ObjectOutputStream oo = new ObjectOutputStream(bo);
	        oo.writeObject(obj);
	        bytes = bo.toByteArray();
	        bo.close();
	        oo.close();
	    } catch (Exception e) {
	        System.out.println("translation" + e.getMessage());
	        e.printStackTrace();
	    }
	    return bytes;
	}
	
	public static Object ByteToObject(byte[] bytes) {
	    Object obj = null;
	    try {
	        // bytearray to object
	        ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
	        ObjectInputStream oi = new ObjectInputStream(bi);
	        obj = oi.readObject();
	        bi.close();
	        oi.close();
	    } catch (Exception e) {
	        System.out.println("translation" + e.getMessage());
	        e.printStackTrace();
	    }
	    return obj;
	}
	
	public static final long getRequestId() {
		return 0L;
	}
	
}
