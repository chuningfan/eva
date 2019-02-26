package eva.core.valve;

public class Result {
	
	private boolean successful;
	
	private String message;
	
	private Exception exception;

	public Result(boolean successful, String message, Exception exception) {
		this.successful = successful;
		this.message = message;
		this.exception = exception;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public Result setSuccessful(boolean successful) {
		this.successful = successful;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public Result setMessage(String message) {
		this.message = message;
		return this;
	}

	public Exception getException() {
		return exception;
	}

	public Result setException(Exception exception) {
		this.exception = exception;
		return this;
	}
	
	public static final Result getDefault(String message) {
		return new Result(false, message, null);	
	}
	
	public static final Result getSuccessful(String message) {
		return new Result(true, message, null);	
	}
	
}
