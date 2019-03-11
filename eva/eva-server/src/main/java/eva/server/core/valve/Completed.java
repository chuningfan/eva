package eva.server.core.valve;

import eva.core.valve.Result;
import eva.core.valve.Valve;
import eva.server.core.handler.ServerParamWrapper;

public class Completed extends Valve<ServerParamWrapper, Result> {

	@Override
	protected Result process0(ServerParamWrapper wrapper, Result result) throws Exception {
		
		
		return Result.getSuccessful("ok");
	}

}
