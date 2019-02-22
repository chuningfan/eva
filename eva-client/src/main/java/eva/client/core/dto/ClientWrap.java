package eva.client.core.dto;

import io.netty.channel.Channel;

public class ClientWrap {
	
	private Channel channel;
	
	private String targetAddress;

	public ClientWrap(Channel channel, String targetAddress) {
		this.channel = channel;
		this.targetAddress = targetAddress;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public String getTargetAddress() {
		return targetAddress;
	}

	public void setTargetAddress(String targetAddress) {
		this.targetAddress = targetAddress;
	}
	
}
