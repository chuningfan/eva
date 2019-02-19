package eva.common.dto;

import java.util.List;

public class ServiceMetadata {
	
	private Class<?> serviceClass;
	
	private List<String> methodInfos;

	public Class<?> getServiceClass() {
		return serviceClass;
	}

	public void setServiceClass(Class<?> serviceClass) {
		this.serviceClass = serviceClass;
	}

	public List<String> getMethodInfos() {
		return methodInfos;
	}

	public void setMethodInfos(List<String> methodInfos) {
		this.methodInfos = methodInfos;
	}
	
}
