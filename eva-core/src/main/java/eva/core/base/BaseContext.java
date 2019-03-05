package eva.core.base;

import java.util.Observable;

import eva.core.exception.EvaContextException;

public abstract class BaseContext<P> extends Observable {

	protected P parameter;
	
	protected BaseContext(P parameter) {
		this.parameter = parameter;
	}
	
	protected abstract void init() throws EvaContextException, InterruptedException;

}
