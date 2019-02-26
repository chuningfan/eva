package eva.core.base;

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
	
	T getSource(Class<?> serviceClass) throws Exception;
	
	T create(Condition condition) throws Exception;
	
	void putback(T source) throws Exception;
}
