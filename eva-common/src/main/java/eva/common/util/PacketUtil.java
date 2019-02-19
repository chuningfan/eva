package eva.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import eva.common.dto.RequestStatus;
import eva.common.transport.Packet;

public class PacketUtil {
	
	/**
	 * except request ID, body size.
	 * @param p
	 */
	public static final void setBodySize(Packet p) {
		int size = 0;
		Object[] args = p.getArgs();
		size += ObjectToByte(args).length;
		Class<?> interfaceClass = p.getInterfaceClass();
		size += ObjectToByte(interfaceClass).length;
		String methodName = p.getMethodName();
		size += methodName.getBytes().length;
		Object result = p.getResponse();
		size += ObjectToByte(result).length;
		Class<?> returnType = p.getReturnType();
		size += ObjectToByte(returnType).length;
		RequestStatus status = p.getStatus();
		size += ObjectToByte(status).length;
		p.setBodySize(size);
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
	
}
