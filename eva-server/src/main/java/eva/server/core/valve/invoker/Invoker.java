package eva.server.core.valve.invoker;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import eva.core.annotation.EvaService;
import eva.core.base.ResourceProvider;
import eva.core.dto.ReturnVoid;
import eva.core.transport.Packet;
import eva.core.transport.Response;
import eva.core.valve.InvokerValve;
import eva.core.valve.Result;
import eva.server.core.handler.ServerParamWrapper;
import eva.server.core.valve.invoker.async.Queue;
import eva.server.core.valve.invoker.async.Task;
import io.netty.channel.ChannelHandlerContext;

public class Invoker extends InvokerValve<ServerParamWrapper, Result> {

	private final ResourceProvider provider;

	public Invoker(ResourceProvider provider) {
		this.provider = provider;
		Collection<Class<?>> evaInterfaces = provider.getEvaInterfaceClasses();
		if (Objects.nonNull(evaInterfaces) && !evaInterfaces.isEmpty()) {
			evaInterfaces.stream().forEach(itf -> {
				Object instance = provider.getSource(itf);
				if (Objects.nonNull(instance)) {
					EvaService evaService = instance.getClass().getAnnotation(EvaService.class);
					if (Objects.nonNull(evaService)) {
						int limit = evaService.accessLimit();
						if (limit > 0) {
							SERVICE_SEM.put(itf, new Semaphore(limit));
						}
					}
				}
			});
		}
	}

	@Override
	protected Result process0(ServerParamWrapper wrapper, Result result) throws Exception {
		Packet packet = wrapper.getPacket();
		ChannelHandlerContext ctx = wrapper.getChannelContext();
		Class<?> interfaceClass = packet.getInterfaceClass();
		if (Objects.isNull(interfaceClass)) {
			return result.setMessage("Pipline > Invoke: No interface class was found!");
		}
		Object proxy = provider.getSource(interfaceClass);
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
					Object res = processInWrap(proxy, method, packet.getArgs());
					resp.setResult(res);
				} else {
					processInWrap(proxy, method, packet.getArgs());
					resp.setResult(ReturnVoid.getInstance());
				}
				resp.setStateCode(0);
				resp.setMessage("ok");
			} else {
				resp.setStateCode(1);
				resp.setMessage("failed");
			}
			if (ctx.channel().isActive() && ctx.channel().isOpen()) {
				ctx.writeAndFlush(resp);
			}
		} else {
			Task task = new Task(packet, ctx);
			Queue.getInstance().addToQueue(task);
		}
		return result.setSuccessful(true).setMessage("ok");
	}

	
}
