package com.example.poc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark correlation ID fields/parameters for automatic extraction.
 * Used in the distributed tracing context as discussed in the chat.
 */
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorrelationId {
    /**
     * The correlation ID field name
     */
    String value() default "correlationId";
    
    /**
     * Whether to generate correlation ID if not present
     */
    boolean generate() default false;
    
    /**
     * HTTP header name to extract correlation ID from
     */
    String headerName() default "X-Correlation-ID";
}