package eva.common.global;

import java.util.Set;

public class RegistryChgData {
	
	public static enum Action {
		REMOVE_SERVICE, REMOVE_ADDRESS, ADD_ADDRESS, ADD_SERVICE;
	}
	
	private String interfaceClassName;
	
	private Action action;
	
	private Set<String> addresses;

	public String getInterfaceClassName() {
		return interfaceClassName;
	}

	public void setInterfaceClassName(String interfaceClassName) {
		this.interfaceClassName = interfaceClassName;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public Set<String> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<String> addresses) {
		this.addresses = addresses;
	}
	
}
