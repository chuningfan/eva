package eva.common.base;

import java.lang.reflect.Method;

import eva.common.exception.EvaAPIException;
import eva.common.exception.EvaContextException;

public interface BaseContext {

	<T> T getBean(Class<T> beanClass);

	void removeBean(Class<?> beanClass);

	void init() throws EvaContextException;

	public static abstract class BaseProxy {

		protected Object target;
		
		protected Class<?> interfaceClass;
		
		protected ClassLoader classLoader;

		protected BaseProxy(Object target, Class<?> interfaceClass, ClassLoader classLoader) {
			this.target = target;
			
			this.interfaceClass = interfaceClass;
			
			this.classLoader = classLoader;
		}

		protected abstract Object getProxy();

		protected abstract Object callFallback(Method method, Object target, String fallbackName, int strategy,
				int retryTime, Object... args) throws EvaAPIException;

	}

}
