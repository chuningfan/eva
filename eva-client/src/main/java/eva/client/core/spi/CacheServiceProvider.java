package eva.client.core.spi;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class CacheServiceProvider {
	
	@SuppressWarnings("rawtypes")
	public static final Cache getCache() {
		return SPIServiceLoader.getServiceInstanceOrDefault(Cache.class, CacheBuilder.newBuilder()
				.expireAfterWrite(60 * 1000, TimeUnit.MILLISECONDS).maximumSize(8 * 1024).build());
	}
	
}
