package eva.server.core.async;

import eva.common.transport.Packet;
import io.netty.channel.ChannelHandlerContext;

public class Task {
	
	private long requestId;
	
	private Packet packet;
	
	private ChannelHandlerContext ctx;
	
	public Task(Packet packet, ChannelHandlerContext ctx) {
		this.requestId = packet.getRequestId();
		this.packet = packet;
		this.ctx = ctx;
	}

	public long getRequestId() {
		return requestId;
	}

	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}

	public Packet getPacket() {
		return packet;
	}

	public void setPacket(Packet packet) {
		this.packet = packet;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}
	
}
