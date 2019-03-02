package eva.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
@Documented
public @interface EvaService {
	
	int maximumConcurrency() default -1;
	
	Class<?> interfaceClass() default Object.class;
	
	int acquireTimeout() default 3000;
	
	TimeUnit acquireTimeUnit() default TimeUnit.MILLISECONDS;
	
	String version() default "";
	
}
