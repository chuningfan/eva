package eva.common.base;

import java.lang.reflect.Method;
import java.util.Observer;

import eva.common.exception.EvaAPIException;

public interface BaseContext extends Observer {

	<T> T getBean(Class<T> beanClass);

	void removeBean(Class<?> beanClass);

	void init();

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
