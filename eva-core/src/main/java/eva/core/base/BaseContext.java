package eva.core.base;

import eva.core.exception.EvaContextException;

public interface BaseContext<A> {

	<T> T getBean(Class<T> beanClass);

	void removeBean(Class<?> beanClass);

	void init(A arg) throws EvaContextException, InterruptedException;

}
