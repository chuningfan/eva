package eva.client.core.spi;

import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class CacheLoader {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final <T extends Cache> T getCache() {
		ServiceLoader<Cache> loader = ServiceLoader.load(Cache.class, CacheLoader.class.getClassLoader());
		Iterator<Cache> itr = loader.iterator();
		T cacheInstance = null;
		while (itr.hasNext()) {
			cacheInstance = (T) itr.next();
			break;
		}
		if (Objects.isNull(cacheInstance)) {
			cacheInstance = (T) CacheBuilder.newBuilder()
					.expireAfterWrite(60 * 1000, TimeUnit.MILLISECONDS).maximumSize(8 * 1024).build();
		}
		return cacheInstance;
	}
	
}
