package com.example.poc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define business operation metadata for comprehensive tracing.
 * Captures business-specific information that goes beyond technical metrics.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessOperation {
    /**
     * Human-readable name of the business operation
     */
    String name();
    
    /**
     * Business category for grouping operations (e.g., "user-management", "payment-processing")
     */
    String category() default "general";
    
    /**
     * Whether this operation handles sensitive data (affects logging and monitoring)
     */
    boolean sensitive() default false;
    
    /**
     * Expected duration category for performance monitoring
     */
    String expectedDuration() default "medium"; // fast, medium, slow
    
    /**
     * Business priority level for operational insights
     */
    String priority() default "normal"; // low, normal, high, critical
    
    /**
     * Business criticality level for incident response
     */
    String criticality() default "normal"; // low, normal, high, critical
}