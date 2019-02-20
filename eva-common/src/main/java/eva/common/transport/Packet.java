package eva.common.transport;

public class Packet {
	
	private long requestId;
	
	private int bodySize;
	
	private Body body = new Body();

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

	public Body getBody() {
		return body;
	}

	public void setBody(Body body) {
		this.body = body;
	}
	
	
}
