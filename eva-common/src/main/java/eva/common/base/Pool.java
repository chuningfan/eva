package eva.common.base;

import java.util.Collection;

import eva.common.exception.EvaClientException;
import io.netty.channel.Channel;
/**
 * This interface is for eva client to pooling connetion resources 
 * 
 * @author Vic.Chu
 *
 * @param <T>
 */
public interface Pool<T> {
	
	T getSource();
	
	void removeSource(T target);
	
	void clear();
	
	Collection<T> getAll();
	
	T getSource(String serverAddress);
	
	Collection<T> getSources(String serverAddress);
	
	Channel create(String providerName) throws EvaClientException;
	
}
