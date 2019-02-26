package eva.server.core.valve;

import java.util.Objects;

import eva.core.valve.Result;
import eva.core.valve.Valve;
import eva.server.core.handler.ServerParamWrapper;

public class PacketChecker extends Valve<ServerParamWrapper, Result> {

	@Override
	protected Result process0(ServerParamWrapper wrapper, Result result) {
		try {
			Objects.requireNonNull(wrapper.getPacket().getRequestId());
			Objects.requireNonNull(wrapper.getPacket().getInterfaceClass());
			Objects.requireNonNull(wrapper.getPacket().getMethodName());
			Objects.requireNonNull(wrapper.getChannelContext());
			Objects.requireNonNull(wrapper.getContext());
		} catch (Exception e) {
			e.printStackTrace();
			return result.setMessage("error").setException(e);
		}
		return result.setSuccessful(true).setMessage("ok");
	}

}
