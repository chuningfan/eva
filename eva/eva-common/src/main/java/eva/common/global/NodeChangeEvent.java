package eva.common.global;

public class NodeChangeEvent {
	
	public static enum Action {
		ADD, DELETE;
	}
	
	private Action action;
	
	private String address;
	
	private String serviceName;

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
}
