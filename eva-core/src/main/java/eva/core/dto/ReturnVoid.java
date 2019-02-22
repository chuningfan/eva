package eva.core.dto;


// This class is for those method whose return type is void
public class ReturnVoid {
	
	private ReturnVoid(){}
	
	private static final class ReturnVoidHolder {
		private static final ReturnVoid INSTANCE = new ReturnVoid();
	}
	
	public static final ReturnVoid getInstance() {
		return ReturnVoidHolder.INSTANCE;
	}
	
}
