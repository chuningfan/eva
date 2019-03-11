package eva.client.core.dto;

import java.io.Serializable;
import java.util.UUID;

import io.netty.channel.Channel;

public class ClientWrapper implements Serializable {
	
	private static final long serialVersionUID = -8823434525171210732L;

	private UUID channelId;
	
	private Channel channel;
	
	private String targetAddress;
	
	private boolean needRecycle = true;

	public ClientWrapper(Channel channel, UUID channelId, String targetAddress) {
		this.channel = channel;
		this.channelId = channelId;
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

	public UUID getChannelId() {
		return channelId;
	}

	public void setChannelId(UUID channelId) {
		this.channelId = channelId;
	}

	public boolean isNeedRecycle() {
		return needRecycle;
	}

	public void setNeedRecycle(boolean needRecycle) {
		this.needRecycle = needRecycle;
	}
	
}
