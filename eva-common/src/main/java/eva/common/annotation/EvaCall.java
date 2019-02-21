package eva.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EvaCall {
	
	int timeout() default 3;
	
	TimeUnit timeUnit() default TimeUnit.SECONDS;
	
	boolean async() default false;
}
