package com.example.poc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark user context fields/parameters for automatic extraction.
 * Based on the user context capture patterns discussed in the chat.
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserContext {
    /**
     * The user context field name
     */
    String value() default "userId";
}