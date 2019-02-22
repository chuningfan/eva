package eva.core.base;

import eva.core.exception.EvaContextException;

public interface BaseContext {

	<T> T getBean(Class<T> beanClass);

	void removeBean(Class<?> beanClass);

	void init() throws EvaContextException;

}
