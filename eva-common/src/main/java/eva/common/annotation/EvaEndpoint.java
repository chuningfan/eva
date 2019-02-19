package eva.common.annotation;

import java.util.concurrent.TimeUnit;

public @interface EvaEndpoint {
	
	public static final int FALLBACK_FAIL_FAST = 0;
	public static final int FALLBACK_RETRY = 1;
	
	
	String fallback() default "";
	
	long timeout() default 3000L;
	
	TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
	
	int maximumConcurrency() default -1;
	
	int acquireTimeout() default 3000;
	
	TimeUnit acquireTimeUnit() default TimeUnit.MILLISECONDS;
	
	int fallbackStrategy() default FALLBACK_FAIL_FAST;
	
	int retryTime() default 3;
	
}
