package eva.client.core.context;

import eva.common.base.AbstractContext;
import eva.common.base.BaseContext;
import eva.common.exception.EvaContextException;

public class EvaClientContext extends AbstractContext implements BaseContext {

	@Override
	public <T> T getBean(Class<T> beanClass) {
		return null;
	}

	@Override
	public void removeBean(Class<?> beanClass) {
		
	}

	@Override
	public void init() throws EvaContextException {
		
	}

}
