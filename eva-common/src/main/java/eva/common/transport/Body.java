package eva.common.transport;

import java.net.SocketAddress;

import eva.common.dto.RequestStatus;

public class Body {
	
	private Class<?> interfaceClass;
	
	private String methodName;
	
	private Object[] args;
	
	private Object response;
	
	private Class<?> returnType;
	
	private RequestStatus status;
	
	private SocketAddress fromAddress;

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

	public SocketAddress getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(SocketAddress fromAddress) {
		this.fromAddress = fromAddress;
	}

}
