package eva.server.core.valve;

import java.util.Objects;
import java.util.logging.Logger;

import eva.core.valve.Result;
import eva.core.valve.Valve;
import eva.server.core.handler.ServerParamWrapper;

public class PacketChecker extends Valve<ServerParamWrapper, Result> {

	private static final Logger LOG = Logger.getLogger("PacketChecker");
	
	@Override
	protected Result process0(ServerParamWrapper wrapper, Result result) {
		try {
			Objects.requireNonNull(wrapper.getPacket().getRequestId());
			Objects.requireNonNull(wrapper.getPacket().getInterfaceClass());
			Objects.requireNonNull(wrapper.getPacket().getMethodName());
			Objects.requireNonNull(wrapper.getChannelContext());
		} catch (Exception e) {
			LOG.warning(e.getMessage());
			return result.setMessage("error").setException(e);
		}
		return result.setSuccessful(true).setMessage("ok");
	}

}
