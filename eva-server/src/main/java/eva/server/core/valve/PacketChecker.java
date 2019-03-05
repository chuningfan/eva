package eva.server.core.valve;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eva.core.valve.Result;
import eva.core.valve.Valve;
import eva.server.core.handler.ServerParamWrapper;

public class PacketChecker extends Valve<ServerParamWrapper, Result> {

	private static final Logger LOG = LoggerFactory.getLogger(PacketChecker.class);
	
	@Override
	protected Result process0(ServerParamWrapper wrapper, Result result) {
		try {
			Objects.requireNonNull(wrapper.getPacket().getRequestId());
			Objects.requireNonNull(wrapper.getPacket().getInterfaceClass());
			Objects.requireNonNull(wrapper.getPacket().getMethodName());
			Objects.requireNonNull(wrapper.getChannelContext());
		} catch (Exception e) {
			LOG.error(e.getMessage());
			return result.setMessage("error").setException(e);
		}
		return result.setSuccessful(true).setMessage("ok");
	}

}
