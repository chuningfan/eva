package eva.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EvaService {
	
	long timeout() default -1L;
	
	TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
	
	Class<?> interfaceClass();
	
	int accessLimit() default -1;
	
}
