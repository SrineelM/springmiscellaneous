package com.example.poc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * =================================================================================================
 * ARCHITECTURAL REVIEW
 * =================================================================================================
 * 
 * The `@UserContext` annotation is a specialized tool for identifying user-related information
 * within method parameters or class fields, so it can be added to the tracing context.
 * 
 * Key Architectural Decisions & Best Practices:
 * ------------------------------------------------
 * 1.  `@Target({ElementType.PARAMETER, ElementType.FIELD})`: This provides flexibility. It can be
 *     used to tag a `userId` parameter in a controller method or a `currentUser` field in a
 *     service class. This covers the most common ways user context is handled.
 * 2.  `@Retention(RetentionPolicy.RUNTIME)`: Correctly configured for runtime introspection by the
 *     AOP aspect.
 * 3.  `String value()`: The `value` attribute, defaulting to "userId", is a sensible choice. It
 *     allows the annotation to be used as a simple marker (`@UserContext`) when the parameter is
 *     named `userId`, but also allows for customization (e.g., `@UserContext("customerId")`) if
 *     the naming convention is different.
 * 
 * Role in the Architecture:
 * -------------------------
 * - This annotation is crucial for tying traces to specific users. This is one of the most
 *   powerful features of a good observability setup, as it allows support teams to instantly
 *   find all the actions performed by a specific user who is reporting an issue.
 * - The `DistributedTracingAspect` will look for this annotation on method parameters. When found,
 *   it will extract the value and add it to the OpenTelemetry Baggage and the logging context (MDC).
 * - By placing the user identifier in Baggage, it can be automatically propagated to all downstream
 *   services, providing a consistent user context across the entire distributed transaction.
 * 
 * Overall Feedback:
 * -----------------
 * - The annotation is simple, focused, and highly effective. It solves a very specific and
 *   important problem in a clean, declarative way.
 * - It helps to standardize how user context is identified and extracted, avoiding the need for
 *   manual, repetitive code to pull user IDs into the tracing context.
 * - The design is clean and follows Java best practices.
 * 
 * This is an excellent example of a small, targeted annotation that provides immense value to the
 * overall observability of the system.
 * =================================================================================================
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserContext {
    /**
     * The user context field name
     */
    String value() default "userId";
}