package eva.server.core.handler;

import java.lang.reflect.Method;
import java.util.Objects;

import eva.common.base.BaseContext;
import eva.common.dto.RequestStatus;
import eva.common.dto.RequestStatus.Status;
import eva.common.transport.Packet;
import eva.common.util.PacketUtil;
import eva.server.core.server.NioServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NioServerHandler extends SimpleChannelInboundHandler<Packet> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		BaseContext CONTEXT = NioServer.getContext();
		Class<?> interfaceClass = packet.getInterfaceClass();
		Object proxy = CONTEXT.getBean(interfaceClass);
		Packet resp = new Packet();
		resp.setRequestId(packet.getRequestId());
		if (Objects.nonNull(proxy)) {
			Class<?>[] types = getTypes(packet.getArgs());
			Method method = null;
			if (Objects.nonNull(types)) {
				method = interfaceClass.getDeclaredMethod(packet.getMethodName(), types);
			} else {
				method = interfaceClass.getDeclaredMethod(packet.getMethodName());
			}
			Class<?> returnType = method.getReturnType();
			if (!"void".equalsIgnoreCase(returnType.getName())) {
				Object res = method.invoke(proxy, packet.getArgs());
				resp.setResponse(res);
			}
			resp.setReturnType(returnType);
			resp.setStatus(new RequestStatus.Builder(Status.SUCCESSFUL, "ok").build());
		} else {
			resp.setStatus(new RequestStatus.Builder(Status.FAILED, "Cannot find proxy instance in context for interface [" + interfaceClass + "]; request ID is " + packet.getRequestId()).build());
		}
		PacketUtil.setBodySize(resp);
		ctx.writeAndFlush(resp);
	}
	
	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}
	
	private Class<?>[] getTypes(Object...args) {
		if (Objects.isNull(args) || args.length == 0) {
			return null;
		}
		Class<?>[] types = new Class<?>[args.length];
		for (int i = 0; i < args.length; i ++) {
			types[i] = args[i].getClass();
		}
		return types;
	}
}
