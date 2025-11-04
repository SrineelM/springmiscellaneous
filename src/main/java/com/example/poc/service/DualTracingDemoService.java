package com.example.poc.service;

import com.example.poc.tracing.TracingUtils;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Demonstrates BOTH OpenTelemetry (programmatic) and Micrometer (@Observed) tracing approaches.
 *
 * <p>This service showcases: 1. **Programmatic OpenTelemetry Tracing**: - Manual span creation -
 * Nested spans - Custom attributes and events - Async/parallel tracing - Context propagation
 *
 * <p>2. **Declarative Micrometer Tracing**: - @Observed annotation - Low-cardinality key-values -
 * Automatic observation creation
 *
 * <p>Key Differences: - OpenTelemetry: Maximum control, rich semantic attributes, vendor-neutral -
 * Micrometer: Spring-native, property-driven, simpler for basic use cases
 *
 * <p>When to use each: - Use OpenTelemetry when you need fine-grained control over spans, custom
 * processors, or vendor-specific features - Use Micrometer when you want Spring autoconfiguration
 * and uniform metrics/tracing abstractions
 *
 * <p>Can they work together? YES! This demo shows both approaches coexisting. Micrometer bridges to
 * OpenTelemetry when micrometer-tracing-bridge-otel is on the classpath.
 */
@Service
public class DualTracingDemoService {
  private static final Logger logger = LoggerFactory.getLogger(DualTracingDemoService.class);

  private final Tracer tracer;

  public DualTracingDemoService(Tracer tracer) {
    this.tracer = tracer;
  }

  // ========================================================================================
  // PROGRAMMATIC OPENTELEMETRY TRACING EXAMPLES
  // ========================================================================================

  /**
   * Demonstrates manual span creation with OpenTelemetry.
   *
   * <p>Use this approach when you need: - Fine-grained control over span lifecycle - Custom span
   * names based on runtime data - Conditional span creation - Rich business-specific attributes
   */
  public String programmaticOtelTracing(String operation) throws Exception {
    logger.info("Executing programmatic OpenTelemetry tracing for: {}", operation);

    // Create a span manually using the utility
    return TracingUtils.withSpan(
        tracer,
        "custom.operation." + operation,
        () -> {
          // Add custom events
          TracingUtils.addEvent("operation.started");

          // Add rich business attributes
          TracingUtils.addAttribute("operation.name", operation);
          TracingUtils.addAttribute("operation.priority", "high");
          TracingUtils.addAttribute("custom.metadata", "business-value-123");

          // Simulate some processing
          simulateWork(100);

          TracingUtils.addEvent("operation.completed");

          return "Programmatic OpenTelemetry span created for: " + operation;
        });
  }

  /**
   * Demonstrates nested spans - parent/child relationship.
   *
   * <p>Useful for tracking sub-operations within a larger workflow.
   */
  public String nestedSpansExample(String workflowId) throws Exception {
    logger.info("Executing nested spans example for workflow: {}", workflowId);

    return TracingUtils.withSpan(
        tracer,
        "workflow.execute",
        () -> {
          TracingUtils.addAttribute("workflow.id", workflowId);
          TracingUtils.addEvent("workflow.started");

          // Child span 1: Data validation
          try {
            String validationResult =
                TracingUtils.withSpan(
                    tracer,
                    "workflow.validate",
                    () -> {
                      TracingUtils.addAttribute("validation.type", "schema");
                      simulateWork(50);
                      return "validated";
                    });

            TracingUtils.addAttribute("validation.result", validationResult);

            // Child span 2: Data processing
            String processingResult =
                TracingUtils.withSpan(
                    tracer,
                    "workflow.process",
                    () -> {
                      TracingUtils.addAttribute("processing.type", "transformation");
                      simulateWork(100);
                      return "processed";
                    });

            TracingUtils.addAttribute("processing.result", processingResult);

            // Child span 3: Data storage
            TracingUtils.withSpan(
                tracer,
                "workflow.store",
                () -> {
                  TracingUtils.addAttribute("storage.type", "database");
                  simulateWork(75);
                  return "stored";
                });
          } catch (Exception e) {
            logger.error("Nested span operation failed", e);
            throw new RuntimeException("Workflow failed", e);
          }

          TracingUtils.addEvent("workflow.completed");
          return "Workflow " + workflowId + " completed with nested spans";
        });
  }

  /**
   * Demonstrates CLIENT span for external service calls.
   *
   * <p>Use SpanKind.CLIENT for outbound requests to external systems.
   */
  public String clientSpanExample(String serviceUrl) throws Exception {
    logger.info("Executing CLIENT span example for: {}", serviceUrl);

    return TracingUtils.withSpan(
        tracer,
        "http.client.call",
        SpanKind.CLIENT,
        () -> {
          // HTTP semantic conventions
          TracingUtils.addAttribute("http.method", "GET");
          TracingUtils.addAttribute("http.url", serviceUrl);
          TracingUtils.addAttribute("http.target", "/api/v1/resource");
          TracingUtils.addAttribute("net.peer.name", "external-service.com");

          TracingUtils.addEvent("http.request.sent");

          // Simulate external call
          simulateWork(200);

          TracingUtils.addAttribute("http.status_code", 200);
          TracingUtils.addEvent("http.response.received");

          return "External call to " + serviceUrl + " completed";
        });
  }

  /**
   * Demonstrates async tracing with context propagation.
   *
   * <p>Shows how to propagate trace context to async/thread pool operations.
   */
  public CompletableFuture<String> asyncTracingExample(String taskId) throws Exception {
    logger.info("Executing async tracing example for task: {}", taskId);

    return TracingUtils.withSpan(
        tracer,
        "async.coordinator",
        () -> {
          TracingUtils.addAttribute("task.id", taskId);
          TracingUtils.addEvent("async.tasks.initiated");

          // Wrap async operation to propagate context
          CompletableFuture<String> future =
              CompletableFuture.supplyAsync(
                  TracingUtils.wrapWithContext(
                      () -> {
                        try {
                          return TracingUtils.withSpan(
                              tracer,
                              "async.task.execute",
                              () -> {
                                TracingUtils.addAttribute("task.type", "background");
                                simulateWork(150);
                                return "Async task " + taskId + " completed";
                              });
                        } catch (Exception e) {
                          logger.error("Async task failed", e);
                          return "Async task failed: " + e.getMessage();
                        }
                      }));

          TracingUtils.addEvent("async.tasks.submitted");
          return future;
        });
  }

  /**
   * Demonstrates custom span builder with fluent API.
   *
   * <p>Shows advanced span configuration.
   */
  public String customSpanBuilderExample(String operationId) throws Exception {
    logger.info("Executing custom span builder example for: {}", operationId);

    return TracingUtils.spanBuilder(tracer, "custom.advanced.operation")
        .withKind(SpanKind.INTERNAL)
        .withAttribute("operation.id", operationId)
        .withAttribute("operation.category", "advanced")
        .withAttribute("operation.version", "2.0")
        .withEvent("operation.initialization")
        .execute(
            () -> {
              simulateWork(100);
              TracingUtils.addEvent("operation.processing");
              simulateWork(50);
              return "Custom span builder executed for: " + operationId;
            });
  }

  /**
   * Demonstrates baggage usage for cross-cutting concerns.
   *
   * <p>Baggage propagates across service boundaries.
   */
  public String baggageExample(String userId, String tenantId) {
    logger.info("Executing baggage example for user: {}, tenant: {}", userId, tenantId);

    Map<String, String> baggageItems =
        Map.of(
            "user.id",
            userId,
            "tenant.id",
            tenantId,
            "feature.flag.enabled",
            "true",
            "priority.level",
            "high");

    return TracingUtils.withBaggage(
        baggageItems,
        () -> {
          // Baggage is now available in current context and will propagate to downstream services
          String userFromBaggage = TracingUtils.getBaggageValue("user.id");
          String tenantFromBaggage = TracingUtils.getBaggageValue("tenant.id");

          logger.info(
              "Baggage retrieved - User: {}, Tenant: {}", userFromBaggage, tenantFromBaggage);

          // Call nested operation - baggage automatically propagates
          return "Baggage propagated for user: "
              + userFromBaggage
              + ", tenant: "
              + tenantFromBaggage;
        });
  }

  // ========================================================================================
  // DECLARATIVE MICROMETER TRACING EXAMPLES
  // ========================================================================================

  /**
   * Demonstrates basic Micrometer @Observed annotation.
   *
   * <p>This is the DECLARATIVE approach - zero code intrusion, pure annotation-based.
   *
   * <p>Advantages: - Simple and clean - Autoconfigured by Spring - Uniform across metrics and
   * tracing
   *
   * <p>When to use: - Standard REST endpoints - Service methods that don't need custom span logic -
   * When you want property-driven configuration
   */
  @Observed(
      name = "micrometer.basic.operation",
      contextualName = "basicMicrometerOperation",
      lowCardinalityKeyValues = {"operation.type", "basic", "framework", "micrometer"})
  public String basicMicrometerTracing(String input) {
    logger.info("Executing basic Micrometer tracing for: {}", input);
    simulateWork(100);
    return "Micrometer observation created for: " + input;
  }

  /**
   * Demonstrates Micrometer with dynamic low-cardinality tags.
   *
   * <p>Note: Use {paramName} syntax to include parameter values as tags. Keep cardinality low
   * (e.g., status codes, not user IDs).
   */
  @Observed(
      name = "micrometer.operation.with.tags",
      lowCardinalityKeyValues = {"operation.id", "{operationId}", "status", "active"})
  public String micrometerWithTags(String operationId) {
    logger.info("Executing Micrometer tracing with tags for: {}", operationId);
    simulateWork(150);
    return "Micrometer operation with tags completed: " + operationId;
  }

  /**
   * Demonstrates nested Micrometer observations.
   *
   * <p>Child observations are automatically linked to parent.
   */
  @Observed(
      name = "micrometer.parent.operation",
      lowCardinalityKeyValues = {"level", "parent"})
  public String nestedMicrometerObservations(String workflowId) {
    logger.info("Executing nested Micrometer observations for: {}", workflowId);

    // Parent observation is active
    simulateWork(50);

    // Call child method (also observed)
    String childResult = childMicrometerOperation(workflowId);

    simulateWork(50);
    return "Nested observations completed: " + childResult;
  }

  @Observed(
      name = "micrometer.child.operation",
      lowCardinalityKeyValues = {"level", "child"})
  private String childMicrometerOperation(String workflowId) {
    logger.info("Executing child Micrometer operation for: {}", workflowId);
    simulateWork(75);
    return "child-" + workflowId;
  }

  // ========================================================================================
  // HYBRID EXAMPLE: BOTH APPROACHES TOGETHER
  // ========================================================================================

  /**
   * Demonstrates using BOTH OpenTelemetry and Micrometer in the same method.
   *
   * <p>This shows they can coexist: - @Observed creates the main span via Micrometer - Manual
   * OpenTelemetry spans add finer-grained details
   *
   * <p>Result: Hierarchical spans with rich context.
   */
  @Observed(
      name = "hybrid.tracing.operation",
      lowCardinalityKeyValues = {"approach", "hybrid"})
  public String hybridTracingExample(String operationId) throws Exception {
    logger.info("Executing hybrid tracing (Micrometer + OpenTelemetry) for: {}", operationId);

    // Micrometer @Observed has created the parent span

    // Now add manual OpenTelemetry child spans for fine-grained control
    String step1 =
        TracingUtils.withSpan(
            tracer,
            "hybrid.step1.validation",
            () -> {
              TracingUtils.addAttribute("step", "validation");
              TracingUtils.addAttribute("validation.rules", "schema,business");
              simulateWork(50);
              return "validated";
            });

    String step2 =
        TracingUtils.withSpan(
            tracer,
            "hybrid.step2.enrichment",
            () -> {
              TracingUtils.addAttribute("step", "enrichment");
              TracingUtils.addAttribute("enrichment.sources", "db,cache,api");
              simulateWork(75);
              return "enriched";
            });

    String step3 =
        TracingUtils.withSpan(
            tracer,
            "hybrid.step3.persistence",
            () -> {
              TracingUtils.addAttribute("step", "persistence");
              TracingUtils.addAttribute("persistence.type", "transactional");
              simulateWork(100);
              return "persisted";
            });

    return String.format(
        "Hybrid tracing completed: %s -> %s -> %s (operation: %s)",
        step1, step2, step3, operationId);
  }

  // ========================================================================================
  // COMPARISON HELPER
  // ========================================================================================

  /** Returns comparison information about OpenTelemetry vs Micrometer. */
  public Map<String, String> getTracingComparison() {
    return Map.of(
        "opentelemetry_approach",
            "Programmatic, vendor-neutral, maximum control, rich semantic attributes",
        "opentelemetry_use_cases",
            "Custom span logic, vendor-specific features, fine-grained control, complex workflows",
        "micrometer_approach", "Declarative, Spring-native, annotation-based, property-driven",
        "micrometer_use_cases",
            "Standard REST endpoints, simple service methods, uniform metrics/tracing",
        "can_they_coexist",
            "YES! micrometer-tracing-bridge-otel bridges Micrometer to OpenTelemetry",
        "recommendation", "Use Micrometer for standard cases, OpenTelemetry for advanced scenarios",
        "best_practice",
            "Choose one as primary, use the other for specific needs (as shown in hybrid example)");
  }

  // ========================================================================================
  // UTILITY METHODS
  // ========================================================================================

  private void simulateWork(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Work interrupted", e);
    }
  }
}
