package eva.server.core.valve;

import java.lang.reflect.Method;
import java.util.Objects;

import eva.core.dto.ReturnVoid;
import eva.core.transport.Packet;
import eva.core.transport.Response;
import eva.core.valve.Result;
import eva.core.valve.Valve;
import eva.server.core.async.Queue;
import eva.server.core.async.Task;
import eva.server.core.handler.ServerParamWrapper;
import io.netty.channel.ChannelHandlerContext;

public class Invoker extends Valve<ServerParamWrapper, Result> {


	@Override
	protected Result process0(ServerParamWrapper wrapper, Result result) throws Exception {
		Packet packet = wrapper.getPacket();
		ChannelHandlerContext ctx = wrapper.getChannelContext();
		Class<?> interfaceClass = packet.getInterfaceClass();
		if (Objects.isNull(interfaceClass)) {
			return result.setMessage("Pipline > Invoke: No interface class was found!");
		}
		Object proxy = wrapper.getContext().getBean(interfaceClass);
		Response resp = new Response();
		resp.setRequestId(packet.getRequestId());
		if (!wrapper.getConfig().isAsyncProcessing()) {
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
			if (ctx.channel().isActive() && ctx.channel().isOpen())
				ctx.writeAndFlush(resp);
		} else {
			Task task = new Task(packet, ctx);
			Queue.getInstance().addToQueue(task);
		}
		return result.setSuccessful(true).setMessage("ok");
	}

}
