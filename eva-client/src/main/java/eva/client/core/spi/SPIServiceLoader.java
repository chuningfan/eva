package eva.client.core.spi;

import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceLoader;

public class SPIServiceLoader {
	
	public static final <T> T getServiceInstanceOrDefault(Class<T> interfaceCLass, T defaultInstance) {
		ServiceLoader<T> loader = ServiceLoader.load(interfaceCLass, SPIServiceLoader.class.getClassLoader());
		Iterator<T> itr = loader.iterator();
		T instance = null;
		while (itr.hasNext()) {
			instance = (T) itr.next();
			break;
		}
		if (Objects.isNull(instance)) {
			instance = defaultInstance;
		}
		return instance;
	}
	
}
