package eva.server.core.handler;

import java.lang.reflect.Method;
import java.util.Objects;

import org.springframework.context.ApplicationContext;

import eva.core.base.config.ServerConfig;
import eva.core.dto.ReturnVoid;
import eva.core.transport.Packet;
import eva.core.transport.Response;
import eva.server.core.async.Queue;
import eva.server.core.async.Task;
import eva.server.core.context.AncientContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@io.netty.channel.ChannelHandler.Sharable
public class NioServerHandler extends SimpleChannelInboundHandler<Packet> {

	private static final ApplicationContext CONTEXT = AncientContext.CONTEXT;
	
	private ServerConfig config;
	
	public NioServerHandler(ServerConfig config) {
		this.config = config;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		Class<?> interfaceClass = packet.getInterfaceClass();
		if (Objects.isNull(interfaceClass)) {
			return;
		}
		Object proxy = CONTEXT.getBean(interfaceClass);
		Response resp = new Response();
		resp.setRequestId(packet.getRequestId());
		if (!config.isAsyncProcessing()) {
			if (Objects.nonNull(proxy)) {
				Class<?>[] types = packet.getArgTypes();
				Method method = null;
				if (Objects.nonNull(types)) {
					method = interfaceClass.getDeclaredMethod(packet.getMethodName(), types);
				} else {
					method = interfaceClass.getDeclaredMethod(packet.getMethodName());
				}
				Class<?> returnType = method.getReturnType();
				if (!"void".equalsIgnoreCase(returnType.getName())) {
					Object res = method.invoke(proxy, packet.getArgs());
					resp.setResult(res);
				} else {
					method.invoke(proxy, packet.getArgs());
					resp.setResult(ReturnVoid.getInstance());
				}
				resp.setStateCode(0);
				resp.setMessage("ok");
			} else {
				resp.setStateCode(1);
				resp.setMessage("failed");
			}
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
		ctx.flush();
		ctx.close();
		throw new Exception(cause);
	}
	
}
