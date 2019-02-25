package eva.client.core.dto;

import java.io.Serializable;

import io.netty.channel.Channel;

public class ClientWrapper implements Serializable {
	
	private static final long serialVersionUID = -8823434525171210732L;

	private Channel channel;
	
	private String targetAddress;

	public ClientWrapper(Channel channel, String targetAddress) {
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
