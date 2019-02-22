package eva.core.base;

import eva.core.exception.EvaClientException;
/**
 * This interface is for eva client to pooling connetion resources 
 * 
 * @author Vic.Chu
 *
 * @param <T>
 */
public interface Pool<T, Condition> {
	
	void removeSource(T target);
	
	void clear();
	
	T getSource(Class<?> serviceClass) throws EvaClientException;
	
	T create(Condition condition) throws EvaClientException;
	
	void putback(T source);
}
