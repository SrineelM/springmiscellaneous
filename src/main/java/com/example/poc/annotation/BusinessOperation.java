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
 * <p>The `@BusinessOperation` annotation is a powerful tool for embedding rich, business-centric
 * metadata directly into the source code. It's a cornerstone of the POC's goal to create
 * meaningful, context-aware distributed traces.
 *
 * <p>Key Architectural Decisions & Best Practices: ------------------------------------------------
 * 1. `@Target(ElementType.METHOD)` and `@Retention(RetentionPolicy.RUNTIME)`: Correctly configured
 * to ensure the annotation is applied to methods and is available for runtime processing by AOP. 2.
 * Comprehensive Metadata: The annotation captures a wide range of business attributes: - `name`: A
 * mandatory, human-readable identifier for the operation. Essential for dashboards. - `category`:
 * Allows for logical grouping of operations (e.g., all "payment" operations). - `sensitive`: A
 * critical flag for security and compliance. The tracing system can use this to automatically
 * redact or mask sensitive data from logs and traces. - `expectedDuration`: Excellent for
 * performance monitoring and anomaly detection. If an operation marked as "fast" takes several
 * seconds, it can trigger an alert. - `priority` & `criticality`: These fields provide crucial
 * context for incident management and SLO/SLI tracking. A failure in a "critical" operation is far
 * more severe than one in a "low" priority operation. 3. `default` values: Providing sensible
 * defaults for most attributes makes the annotation easier to use. Developers only need to specify
 * what deviates from the norm, reducing verbosity.
 *
 * <p>Role in the Architecture: ------------------------- - This annotation is the primary source of
 * business context for the `DistributedTracingAspect`. - It transforms a simple technical trace
 * (e.g., "method X was called") into a rich business event (e.g., "A high-criticality payment
 * processing operation, which is expected to be fast, was executed."). - The metadata can be added
 * as tags/attributes to spans in OpenTelemetry, making traces filterable and searchable on business
 * terms.
 *
 * <p>Overall Feedback: ----------------- - This is an exceptionally well-designed annotation that
 * demonstrates a mature approach to observability. It understands that raw technical data is not
 * enough; context is king. - It promotes a culture of "observability-driven development," where
 * business and operational concerns are considered part of the implementation. - The choice of
 * attributes is thoughtful and covers key aspects of business operations: identity, grouping,
 * security, performance, and importance.
 *
 * <p>This annotation is a prime example of how to build a powerful and insightful distributed
 * tracing system.
 * =================================================================================================
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessOperation {
  /** Human-readable name of the business operation */
  String name();

  /** Business category for grouping operations (e.g., "user-management", "payment-processing") */
  String category() default "general";

  /** Whether this operation handles sensitive data (affects logging and monitoring) */
  boolean sensitive() default false;

  /** Expected duration category for performance monitoring */
  String expectedDuration() default "medium"; // fast, medium, slow

  /** Business priority level for operational insights */
  String priority() default "normal"; // low, normal, high, critical

  /** Business criticality level for incident response */
  String criticality() default "normal"; // low, normal, high, critical
}
