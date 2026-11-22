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
 * <p>The `@CorrelationId` annotation is a specialized tool for managing correlation IDs, a
 * fundamental concept in distributed systems for tracking a request's journey across multiple
 * services.
 *
 * <p>Key Architectural Decisions & Best Practices: ------------------------------------------------
 * 1. `@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})`: This is a flexible
 * and powerful design. It allows the correlation ID to be sourced from various places: -
 * `PARAMETER`: To extract it from a method argument. - `FIELD`: To associate it with a class
 * member. - `METHOD`: To apply logic at the method level, such as generating a new ID. 2.
 * `@Retention(RetentionPolicy.RUNTIME)`: Correctly set to allow runtime processing by the AOP
 * aspect. 3. Attributes for Flexibility: - `value()`: Provides a name for the correlation ID,
 * defaulting to a sensible "correlationId". - `generate()`: This boolean flag is a key feature. It
 * allows the system to be self-healing. If an incoming request lacks a correlation ID, the system
 * can automatically generate one, ensuring that tracing is never lost. - `headerName()`: This
 * directly links the correlation ID to the transport layer (HTTP), specifying which header to
 * inspect. The default `X-Correlation-ID` is a common convention.
 *
 * <p>Role in the Architecture: ------------------------- - This annotation is central to the
 * "context propagation" aspect of distributed tracing. It provides a declarative way to manage the
 * lifecycle of a correlation ID. - The `DistributedTracingAspect` will use this annotation to: 1.
 * Look for the correlation ID in the specified HTTP header. 2. If not found, check if it should be
 * generated. 3. Place the correlation ID into the logging context (MDC) and the OpenTelemetry
 * Baggage. - This ensures that the correlation ID is consistently available for logging and is
 * propagated to downstream services.
 *
 * <p>Overall Feedback: ----------------- - The design is robust and covers the common scenarios for
 * correlation ID management (receiving, generating, and propagating). - The flexibility of the
 * `@Target` is a major strength, allowing it to adapt to different coding styles and requirements.
 * - The combination of `generate()` and `headerName()` provides a complete, declarative mechanism
 * for handling correlation IDs at the edge of the service.
 *
 * <p>This annotation is a well-thought-out component that greatly simplifies the complex task of
 * managing correlation IDs in a distributed environment.
 * =================================================================================================
 */
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorrelationId {
  /** The correlation ID field name */
  String value() default "correlationId";

  /** Whether to generate correlation ID if not present */
  boolean generate() default false;

  /** HTTP header name to extract correlation ID from */
  String headerName() default "X-Correlation-ID";
}
