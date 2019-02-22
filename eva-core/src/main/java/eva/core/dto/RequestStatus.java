package eva.core.dto;

public class RequestStatus {
	
	private Status status;
	
	private String message;
	
	public RequestStatus(Status status, String message) {
		this.status = status;
		this.message = message;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public static final class Builder {
		
		private Status status;
		private String message;
		public Builder(Status status, String message) {
			this.status = status;
			this.message = message;
		}
		
		public final RequestStatus build() {
			return new RequestStatus(status, message);
		}
	}
	
	public enum Status {
		SUCCESSFUL(0), FAILED(-1);
		
		private int code;
		
		private Status(int code) {
			this.code = code;
		}
	
		public int getCode() {
			return code;
		}
	
		public void setCode(int code) {
			this.code = code;
		}
	}
}
