package eva.server.core.handler;

import org.springframework.context.ApplicationContext;

import eva.core.base.config.ServerConfig;
import eva.core.transport.Packet;
import eva.core.valve.EvaPipeline;
import eva.core.valve.EvaPipeline.Direction;
import eva.core.valve.Result;
import eva.server.core.context.AncientContext;
import eva.server.core.valve.Completed;
import eva.server.core.valve.Invoker;
import eva.server.core.valve.PacketChecker;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@io.netty.channel.ChannelHandler.Sharable
public class NioServerHandler extends SimpleChannelInboundHandler<Packet> {

	private static final ApplicationContext CONTEXT = AncientContext.CONTEXT;

	private ServerConfig config;

	private static EvaPipeline<ServerParamWrapper, Result> PIPELINE = null;
	
	static {
		PIPELINE = new EvaPipeline<ServerParamWrapper, Result>();
		PIPELINE.addLast(new PacketChecker()).addLast(new Invoker()).addLast(new Completed());
	}

	public NioServerHandler(ServerConfig config) {
		this.config = config;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		ServerParamWrapper wrapper = new ServerParamWrapper(CONTEXT, config, packet, ctx);
		Result res = PIPELINE.doProcess(Direction.FORWARD, wrapper, Result.getDefault(null));
		if (!res.isSuccessful()) {
			throw res.getException();
		}
		// Class<?> interfaceClass = packet.getInterfaceClass();
		// if (Objects.isNull(interfaceClass)) {
		// return;
		// }
		// Object proxy = CONTEXT.getBean(interfaceClass);
		// Response resp = new Response();
		// resp.setRequestId(packet.getRequestId());
		// if (!config.isAsyncProcessing()) {
		// if (Objects.nonNull(proxy)) {
		// Class<?>[] types = packet.getArgTypes();
		// Method method = null;
		// if (Objects.nonNull(types)) {
		// method = interfaceClass.getDeclaredMethod(packet.getMethodName(),
		// types);
		// } else {
		// method = interfaceClass.getDeclaredMethod(packet.getMethodName());
		// }
		// Class<?> returnType = method.getReturnType();
		// if (!"void".equalsIgnoreCase(returnType.getName())) {
		// Object res = method.invoke(proxy, packet.getArgs());
		// resp.setResult(res);
		// } else {
		// method.invoke(proxy, packet.getArgs());
		// resp.setResult(ReturnVoid.getInstance());
		// }
		// resp.setStateCode(0);
		// resp.setMessage("ok");
		// } else {
		// resp.setStateCode(1);
		// resp.setMessage("failed");
		// }
		// if (ctx.channel().isActive() && ctx.channel().isOpen())
		// ctx.writeAndFlush(resp);
		// } else {
		// Task task = new Task(packet, ctx);
		// Queue.getInstance().addToQueue(task);
		// }
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
