package com.example.poc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class with a specific feature name for tracing context.
 * Used to group related operations under a business feature umbrella.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureName {
    /**
     * The business feature name (e.g., "user-management", "order-processing")
     */
    String value();
}