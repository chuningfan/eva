package eva.core.base;

import java.util.Collection;

public interface ResourceProvider {
	
	Object getSource(Class<?> interfaceClass);
	
	Collection<Class<?>> getEvaInterfaceClasses();
	
}
