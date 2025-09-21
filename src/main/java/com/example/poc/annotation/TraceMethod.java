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
 * The `@TraceMethod` annotation provides fine-grained control over how a specific method is traced.
 * It acts as a more technical, detailed counterpart to the business-focused `@BusinessOperation`.
 * 
 * Key Architectural Decisions & Best Practices:
 * ------------------------------------------------
 * 1.  `@Target(ElementType.METHOD)` and `@Retention(RetentionPolicy.RUNTIME)`: Correctly scoped for
 *     method-level tracing and runtime introspection.
 * 2.  Granular Control Attributes:
 *     - `operationName()`: Allows overriding the default span name (which is often the method name).
 *       This is useful for creating more descriptive or standardized span names.
 *     - `includeArgs()`: A powerful but potentially risky feature. It's excellent for debugging,
 *       as it allows you to see the exact inputs to a method directly in the trace. However, it
 *       must be used with caution to avoid logging sensitive data (like passwords or personal
 *       information). The `default false` is a very smart and secure default.
 *     - `includeReturnValue()`: Similar to `includeArgs`, this is great for debugging but requires
 *       care regarding sensitive data. The secure default is appropriate.
 *     - `tags()`: Provides a flexible way to add arbitrary key-value pairs to a span. This is
 *       perfect for adding context that is specific to a single method call and doesn't fit into
 *       the broader business context.
 * 
 * Role in the Architecture:
 * -------------------------
 * - While `@BusinessOperation` defines the "what" and "why" from a business perspective, `@TraceMethod`
 *   defines the "how" from a technical tracing perspective.
 * - It gives developers precise control over the level of detail in their traces. For a critical,
 *   complex method, a developer might enable `includeArgs` and `includeReturnValue` during a
 *   debugging session. For a simple, high-volume method, they would leave them disabled to reduce
 *   noise and overhead.
 * - The `DistributedTracingAspect` will use this annotation to customize the creation and enrichment
 *   of OpenTelemetry spans.
 * 
 * Overall Feedback:
 * -----------------
 * - This annotation is well-designed, providing a good balance between power and safety (thanks to
 *   secure defaults).
 * - It empowers developers to tailor the observability of their code without cluttering the business
 *   logic with tracing-specific calls.
 * - The combination of this annotation with `@BusinessOperation` allows for a multi-layered
 *   approach to tracing, capturing both high-level business context and low-level technical details.
 * 
 * This is a key component for enabling detailed, developer-centric tracing within the application.
 * =================================================================================================
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