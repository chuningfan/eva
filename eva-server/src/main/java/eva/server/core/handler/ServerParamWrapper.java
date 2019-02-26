package eva.server.core.handler;

import org.springframework.context.ApplicationContext;

import eva.core.base.config.ServerConfig;
import eva.core.transport.Packet;
import io.netty.channel.ChannelHandlerContext;

public class ServerParamWrapper {
	
	private ApplicationContext context;
	
	private ServerConfig config;
	
	private Packet packet;
	
	private ChannelHandlerContext channelContext;

	public ServerParamWrapper(ApplicationContext context, ServerConfig config, Packet packet,
			ChannelHandlerContext channelContext) {
		this.context = context;
		this.config = config;
		this.packet = packet;
		this.channelContext = channelContext;
	}

	public ApplicationContext getContext() {
		return context;
	}

	public void setContext(ApplicationContext context) {
		this.context = context;
	}

	public ServerConfig getConfig() {
		return config;
	}

	public void setConfig(ServerConfig config) {
		this.config = config;
	}

	public Packet getPacket() {
		return packet;
	}

	public void setPacket(Packet packet) {
		this.packet = packet;
	}

	public ChannelHandlerContext getChannelContext() {
		return channelContext;
	}

	public void setChannelContext(ChannelHandlerContext channelContext) {
		this.channelContext = channelContext;
	}
	
}
