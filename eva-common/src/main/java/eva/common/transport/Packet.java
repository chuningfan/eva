package eva.common.transport;

import eva.common.dto.RequestStatus;

public class Packet {
	
	private long requestId;
	
	private int bodySize;
	
	private Class<?> interfaceClass;
	
	private String methodName;
	
	private Object[] args;
	
	private Object response;
	
	private Class<?> returnType;
	
	private RequestStatus status;

	public long getRequestId() {
		return requestId;
	}

	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}

	public int getBodySize() {
		return bodySize;
	}

	public void setBodySize(int bodySize) {
		this.bodySize = bodySize;
	}

	public Class<?> getInterfaceClass() {
		return interfaceClass;
	}

	public void setInterfaceClass(Class<?> interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public Object getResponse() {
		return response;
	}

	public void setResponse(Object response) {
		this.response = response;
	}

	public Class<?> getReturnType() {
		return returnType;
	}

	public void setReturnType(Class<?> returnType) {
		this.returnType = returnType;
	}

	public RequestStatus getStatus() {
		return status;
	}

	public void setStatus(RequestStatus status) {
		this.status = status;
	}
	
}
