package com.example.poc.tracing;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for distributed tracing operations. Provides helper methods for: - Creating and
 * managing spans programmatically - Extracting trace context - Working with baggage - Async tracing
 * patterns - Error handling in traced operations
 *
 * <p>This class complements the declarative AOP approach with programmatic tracing capabilities for
 * scenarios where annotations aren't suitable (dynamic spans, conditional tracing, etc.)
 */
public class TracingUtils {
  private static final Logger logger = LoggerFactory.getLogger(TracingUtils.class);

  private TracingUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Gets the current active span.
   *
   * @return Current span, or an invalid span if no span is active
   */
  public static Span getCurrentSpan() {
    return Span.current();
  }

  /**
   * Checks if there's an active span in the current context.
   *
   * @return true if there's a valid span active
   */
  public static boolean hasActiveSpan() {
    return Span.current().getSpanContext().isValid();
  }

  /**
   * Gets the current trace ID.
   *
   * @return Trace ID as hex string, or empty string if no active span
   */
  public static String getCurrentTraceId() {
    Span span = Span.current();
    return span.getSpanContext().isValid() ? span.getSpanContext().getTraceId() : "";
  }

  /**
   * Gets the current span ID.
   *
   * @return Span ID as hex string, or empty string if no active span
   */
  public static String getCurrentSpanId() {
    Span span = Span.current();
    return span.getSpanContext().isValid() ? span.getSpanContext().getSpanId() : "";
  }

  /**
   * Creates a child span with the given name and executes the operation within its scope.
   *
   * @param tracer The tracer instance
   * @param spanName Name of the span
   * @param operation Operation to execute within the span
   * @param <T> Return type of the operation
   * @return Result of the operation
   * @throws Exception if the operation throws
   */
  public static <T> T withSpan(Tracer tracer, String spanName, Supplier<T> operation)
      throws Exception {
    return withSpan(tracer, spanName, SpanKind.INTERNAL, operation);
  }

  /**
   * Creates a child span with specified kind and executes the operation within its scope.
   *
   * @param tracer The tracer instance
   * @param spanName Name of the span
   * @param spanKind Kind of span (CLIENT, SERVER, INTERNAL, etc.)
   * @param operation Operation to execute within the span
   * @param <T> Return type of the operation
   * @return Result of the operation
   * @throws Exception if the operation throws
   */
  public static <T> T withSpan(
      Tracer tracer, String spanName, SpanKind spanKind, Supplier<T> operation) throws Exception {
    Span span = tracer.spanBuilder(spanName).setSpanKind(spanKind).startSpan();

    try (Scope scope = span.makeCurrent()) {
      T result = operation.get();
      span.setStatus(StatusCode.OK);
      return result;
    } catch (Exception e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }

  /**
   * Creates a span with custom attributes and executes the operation.
   *
   * @param tracer The tracer instance
   * @param spanName Name of the span
   * @param attributes Map of attribute key-value pairs to add to the span
   * @param operation Operation to execute
   * @param <T> Return type
   * @return Result of the operation
   * @throws Exception if the operation throws
   */
  public static <T> T withSpanAndAttributes(
      Tracer tracer, String spanName, Map<String, String> attributes, Supplier<T> operation)
      throws Exception {
    Span span = tracer.spanBuilder(spanName).setSpanKind(SpanKind.INTERNAL).startSpan();

    // Add all attributes to the span
    attributes.forEach(span::setAttribute);

    try (Scope scope = span.makeCurrent()) {
      T result = operation.get();
      span.setStatus(StatusCode.OK);
      return result;
    } catch (Exception e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }

  /**
   * Creates a span and provides it to a consumer for custom configuration before execution.
   *
   * @param tracer The tracer instance
   * @param spanName Name of the span
   * @param configurator Consumer to configure the span builder
   * @param operation Operation to execute
   * @param <T> Return type
   * @return Result of the operation
   * @throws Exception if the operation throws
   */
  public static <T> T withConfiguredSpan(
      Tracer tracer, String spanName, Consumer<SpanBuilder> configurator, Supplier<T> operation)
      throws Exception {
    SpanBuilder spanBuilder = tracer.spanBuilder(spanName);
    configurator.accept(spanBuilder);

    Span span = spanBuilder.startSpan();

    try (Scope scope = span.makeCurrent()) {
      T result = operation.get();
      span.setStatus(StatusCode.OK);
      return result;
    } catch (Exception e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }

  /**
   * Executes an operation without creating a new span, but provides access to the current span for
   * adding events/attributes.
   *
   * @param operation Function that receives the current span and returns a result
   * @param <T> Return type
   * @return Result of the operation
   */
  public static <T> T withCurrentSpan(Function<Span, T> operation) {
    Span currentSpan = Span.current();
    return operation.apply(currentSpan);
  }

  /**
   * Adds an event to the current span.
   *
   * @param eventName Name of the event
   */
  public static void addEvent(String eventName) {
    Span.current().addEvent(eventName);
  }

  /**
   * Adds an event with attributes to the current span.
   *
   * @param eventName Name of the event
   * @param attributes Event attributes
   */
  public static void addEvent(String eventName, Map<String, String> attributes) {
    Span span = Span.current();
    io.opentelemetry.api.common.AttributesBuilder attrBuilder =
        io.opentelemetry.api.common.Attributes.builder();
    attributes.forEach(attrBuilder::put);
    span.addEvent(eventName, attrBuilder.build());
  }

  /**
   * Adds an attribute to the current span.
   *
   * @param key Attribute key
   * @param value Attribute value
   */
  public static void addAttribute(String key, String value) {
    Span.current().setAttribute(key, value);
  }

  /**
   * Adds an attribute to the current span.
   *
   * @param key Attribute key
   * @param value Attribute value (long)
   */
  public static void addAttribute(String key, long value) {
    Span.current().setAttribute(key, value);
  }

  /**
   * Adds an attribute to the current span.
   *
   * @param key Attribute key
   * @param value Attribute value (boolean)
   */
  public static void addAttribute(String key, boolean value) {
    Span.current().setAttribute(key, value);
  }

  /**
   * Adds multiple attributes to the current span.
   *
   * @param attributes Map of attributes to add
   */
  public static void addAttributes(Map<String, String> attributes) {
    Span span = Span.current();
    attributes.forEach(span::setAttribute);
  }

  /**
   * Records an exception in the current span without setting error status.
   *
   * @param exception The exception to record
   */
  public static void recordException(Throwable exception) {
    Span.current().recordException(exception);
  }

  /**
   * Records an exception and sets the span status to ERROR.
   *
   * @param exception The exception to record
   */
  public static void recordExceptionAndSetError(Throwable exception) {
    Span span = Span.current();
    span.recordException(exception);
    span.setStatus(StatusCode.ERROR, exception.getMessage());
  }

  /**
   * Gets a value from the current baggage.
   *
   * @param key Baggage key
   * @return Baggage value, or null if not found
   */
  public static String getBaggageValue(String key) {
    return Baggage.current().getEntryValue(key);
  }

  /**
   * Sets a baggage value in the current context.
   *
   * @param key Baggage key
   * @param value Baggage value
   * @return New baggage instance with the added value
   */
  public static Baggage setBaggageValue(String key, String value) {
    return Baggage.current().toBuilder().put(key, value).build();
  }

  /**
   * Executes an operation with additional baggage items.
   *
   * @param baggageItems Map of baggage key-value pairs
   * @param operation Operation to execute
   * @param <T> Return type
   * @return Result of the operation
   */
  public static <T> T withBaggage(Map<String, String> baggageItems, Supplier<T> operation) {
    io.opentelemetry.api.baggage.BaggageBuilder builder = Baggage.current().toBuilder();
    baggageItems.forEach(builder::put);
    Baggage baggage = builder.build();

    try (Scope scope = baggage.makeCurrent()) {
      return operation.get();
    }
  }

  /**
   * Creates a detached context with a new span (useful for async operations).
   *
   * @param tracer The tracer instance
   * @param spanName Name of the span
   * @return A new context with the span
   */
  public static Context createDetachedContext(Tracer tracer, String spanName) {
    Span span =
        tracer.spanBuilder(spanName).setSpanKind(SpanKind.INTERNAL).setNoParent().startSpan();
    return Context.current().with(span);
  }

  /**
   * Creates a new ROOT span (not a child of current span).
   *
   * @param tracer The tracer instance
   * @param spanName Name of the span
   * @param operation Operation to execute
   * @param <T> Return type
   * @return Result of the operation
   * @throws Exception if the operation throws
   */
  public static <T> T withRootSpan(Tracer tracer, String spanName, Supplier<T> operation)
      throws Exception {
    Span span = tracer.spanBuilder(spanName).setNoParent().startSpan();

    try (Scope scope = span.makeCurrent()) {
      T result = operation.get();
      span.setStatus(StatusCode.OK);
      return result;
    } catch (Exception e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }

  /**
   * Wraps a Runnable with tracing context propagation (useful for async/thread pool execution).
   *
   * @param runnable The runnable to wrap
   * @return A context-aware runnable
   */
  public static Runnable wrapWithContext(Runnable runnable) {
    Context context = Context.current();
    return () -> {
      try (Scope scope = context.makeCurrent()) {
        runnable.run();
      }
    };
  }

  /**
   * Wraps a Supplier with tracing context propagation.
   *
   * @param supplier The supplier to wrap
   * @param <T> Return type
   * @return A context-aware supplier
   */
  public static <T> Supplier<T> wrapWithContext(Supplier<T> supplier) {
    Context context = Context.current();
    return () -> {
      try (Scope scope = context.makeCurrent()) {
        return supplier.get();
      }
    };
  }

  /**
   * Safely executes an operation and logs any tracing errors without propagating them.
   *
   * @param operation Operation to execute
   * @param <T> Return type
   * @return Result of the operation, or null if it threw an exception
   */
  public static <T> T safeTrace(Supplier<T> operation) {
    try {
      return operation.get();
    } catch (Exception e) {
      logger.warn("Tracing operation failed, but application logic continues", e);
      return null;
    }
  }

  /**
   * Builder for creating spans with a fluent API.
   *
   * <pre>{@code
   * TracingUtils.spanBuilder(tracer, "my-operation")
   *     .withKind(SpanKind.CLIENT)
   *     .withAttribute("key", "value")
   *     .withEvent("processing-start")
   *     .execute(() -> performOperation());
   * }</pre>
   */
  public static class SpanBuilderHelper {
    private final Tracer tracer;
    private final String spanName;
    private SpanKind spanKind = SpanKind.INTERNAL;
    private final Map<String, String> attributes = new java.util.HashMap<>();
    private final java.util.List<String> events = new java.util.ArrayList<>();

    private SpanBuilderHelper(Tracer tracer, String spanName) {
      this.tracer = tracer;
      this.spanName = spanName;
    }

    public SpanBuilderHelper withKind(SpanKind kind) {
      this.spanKind = kind;
      return this;
    }

    public SpanBuilderHelper withAttribute(String key, String value) {
      this.attributes.put(key, value);
      return this;
    }

    public SpanBuilderHelper withEvent(String eventName) {
      this.events.add(eventName);
      return this;
    }

    public <T> T execute(Supplier<T> operation) throws Exception {
      Span span = tracer.spanBuilder(spanName).setSpanKind(spanKind).startSpan();

      // Add attributes
      attributes.forEach(span::setAttribute);

      try (Scope scope = span.makeCurrent()) {
        // Add events
        events.forEach(span::addEvent);

        T result = operation.get();
        span.setStatus(StatusCode.OK);
        return result;
      } catch (Exception e) {
        span.recordException(e);
        span.setStatus(StatusCode.ERROR, e.getMessage());
        throw e;
      } finally {
        span.end();
      }
    }

    public void execute(Runnable operation) throws Exception {
      execute(
          () -> {
            operation.run();
            return null;
          });
    }
  }

  /**
   * Creates a new span builder helper.
   *
   * @param tracer The tracer instance
   * @param spanName Name of the span
   * @return A new span builder helper
   */
  public static SpanBuilderHelper spanBuilder(Tracer tracer, String spanName) {
    return new SpanBuilderHelper(tracer, spanName);
  }
}
