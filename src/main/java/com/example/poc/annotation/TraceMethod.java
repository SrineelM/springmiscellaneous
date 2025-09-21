package com.example.poc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable tracing on specific methods with custom configuration.
 * Based on the tracing patterns discussed in the chat.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceMethod {
    /**
     * Custom operation name for the span
     */
    String operationName() default "";
    
    /**
     * Whether to include method arguments in trace attributes
     */
    boolean includeArgs() default false;
    
    /**
     * Whether to include return value in trace attributes
     */
    boolean includeReturnValue() default false;
    
    /**
     * Additional custom tags to add to the span
     */
    String[] tags() default {};
}