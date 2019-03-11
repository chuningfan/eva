package eva.core.dto;

public class NoSemaphore {
	
	private NoSemaphore() {
	}
	
	private static final class NoSemaphoreHolder {
		private static final NoSemaphore INSTANCE = new NoSemaphore();
	}
	
	public static final NoSemaphore getInstance() {
		return NoSemaphoreHolder.INSTANCE;
	}
	
}
