package eva.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EvaDoll {
	
	String fallback() default "";
	
	long timeout() default -1L;
	
	TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
	
}
