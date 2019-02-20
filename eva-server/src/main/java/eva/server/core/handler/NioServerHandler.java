package eva.server.core.handler;

import java.lang.reflect.Method;
import java.util.Objects;

import org.springframework.context.ApplicationContext;

import eva.common.base.config.ServerConfig;
import eva.common.dto.RequestStatus;
import eva.common.dto.RequestStatus.Status;
import eva.common.transport.Packet;
import eva.common.util.PacketUtil;
import eva.server.core.async.Queue;
import eva.server.core.async.Task;
import eva.server.core.context.AncientContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NioServerHandler extends SimpleChannelInboundHandler<Packet> {

	private ServerConfig config;
	
	public NioServerHandler(ServerConfig config) {
		this.config = config;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		ApplicationContext CONTEXT = AncientContext.CONTEXT;
		Class<?> interfaceClass = packet.getBody().getInterfaceClass();
		Object proxy = CONTEXT.getBean(interfaceClass);
		Packet resp = new Packet();
		resp.setRequestId(packet.getRequestId());
		if (!config.isAsyncProcessing()) {
			if (Objects.nonNull(proxy)) {
				Class<?>[] types = PacketUtil.getTypes(packet.getBody().getArgs());
				Method method = null;
				if (Objects.nonNull(types)) {
					method = interfaceClass.getDeclaredMethod(packet.getBody().getMethodName(), types);
				} else {
					method = interfaceClass.getDeclaredMethod(packet.getBody().getMethodName());
				}
				Class<?> returnType = method.getReturnType();
				if (!"void".equalsIgnoreCase(returnType.getName())) {
					Object res = method.invoke(proxy, packet.getBody().getArgs());
					resp.getBody().setResponse(res);
				}
				resp.getBody().setReturnType(returnType);
				resp.getBody().setStatus(new RequestStatus.Builder(Status.SUCCESSFUL, "ok").build());
			} else {
				resp.getBody().setStatus(new RequestStatus.Builder(Status.FAILED, "Cannot find proxy instance in context for interface [" + interfaceClass + "]; request ID is " + packet.getRequestId()).build());
			}
			PacketUtil.setBodySize(resp);
			ctx.writeAndFlush(resp);
		} else {
			Task task = new Task(packet, ctx);
			Queue.getInstance().addToQueue(task);
		}
	}
	
	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}
	
}
