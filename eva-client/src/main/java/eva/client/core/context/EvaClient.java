package eva.client.core.context;

import java.util.Observable;

import eva.common.base.AbstractContext;
import eva.common.base.BaseContext;
import eva.common.exception.EvaContextException;

public class EvaClient extends AbstractContext implements BaseContext {

	@Override
	public void update(Observable arg0, Object arg1) {
		
	}

	@Override
	public <T> T getBean(Class<T> beanClass) {
		return null;
	}

	@Override
	public void removeBean(Class<?> beanClass) {
		
	}

	@Override
	public void init() throws EvaContextException {
		// TODO Auto-generated method stub
		
	}

}
