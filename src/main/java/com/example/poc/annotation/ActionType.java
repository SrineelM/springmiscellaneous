package com.example.poc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify action type for business operations.
 * Used to categorize operations for tracing and monitoring.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActionType {
    /**
     * The action type (e.g., "CREATE", "READ", "UPDATE", "DELETE", "PROCESS")
     */
    String value();
}