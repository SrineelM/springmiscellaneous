package com.example.poc.controller;

import com.example.poc.model.ProcessingResult;
import com.example.poc.service.DualTracingDemoService;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller demonstrating both OpenTelemetry and Micrometer tracing approaches.
 *
 * <p>This controller provides endpoints to compare: - **Programmatic OpenTelemetry**: Manual span
 * creation with full control - **Declarative Micrometer**: Annotation-based observations - **Hybrid
 * Approach**: Both working together
 *
 * <p>Use these endpoints to understand when to use each approach and how they complement each
 * other.
 */
@RestController
@RequestMapping("/api/v1/tracing")
public class DualTracingDemoController {
  private static final Logger logger = LoggerFactory.getLogger(DualTracingDemoController.class);

  private final DualTracingDemoService demoService;

  public DualTracingDemoController(DualTracingDemoService demoService) {
    this.demoService = demoService;
  }

  // ========================================================================================
  // OPENTELEMETRY PROGRAMMATIC TRACING ENDPOINTS
  // ========================================================================================

  /**
   * Demonstrates programmatic OpenTelemetry tracing.
   *
   * <p>Endpoint: GET /api/v1/tracing/otel/programmatic?operation=data-processing
   */
  @GetMapping("/otel/programmatic")
  public ResponseEntity<ProcessingResult> testProgrammaticOtel(
      @RequestParam(defaultValue = "sample-operation") String operation) {
    logger.info("Testing programmatic OpenTelemetry tracing");

    try {
      String result = demoService.programmaticOtelTracing(operation);
      return ResponseEntity.ok(buildResult("otel-programmatic", result, "OpenTelemetry"));
    } catch (Exception e) {
      logger.error("Programmatic OTel tracing failed", e);
      return ResponseEntity.internalServerError().body(buildError("otel-programmatic", e));
    }
  }

  /**
   * Demonstrates nested spans with parent/child relationships.
   *
   * <p>Endpoint: GET /api/v1/tracing/otel/nested?workflowId=wf-123
   */
  @GetMapping("/otel/nested")
  public ResponseEntity<ProcessingResult> testNestedSpans(
      @RequestParam(defaultValue = "workflow-001") String workflowId) {
    logger.info("Testing nested OpenTelemetry spans");

    try {
      String result = demoService.nestedSpansExample(workflowId);
      return ResponseEntity.ok(buildResult("otel-nested", result, "OpenTelemetry"));
    } catch (Exception e) {
      logger.error("Nested spans test failed", e);
      return ResponseEntity.internalServerError().body(buildError("otel-nested", e));
    }
  }

  /**
   * Demonstrates CLIENT span for external service calls.
   *
   * <p>Endpoint: GET /api/v1/tracing/otel/client?serviceUrl=https://api.example.com
   */
  @GetMapping("/otel/client")
  public ResponseEntity<ProcessingResult> testClientSpan(
      @RequestParam(defaultValue = "https://api.example.com") String serviceUrl) {
    logger.info("Testing CLIENT span");

    try {
      String result = demoService.clientSpanExample(serviceUrl);
      return ResponseEntity.ok(buildResult("otel-client", result, "OpenTelemetry"));
    } catch (Exception e) {
      logger.error("CLIENT span test failed", e);
      return ResponseEntity.internalServerError().body(buildError("otel-client", e));
    }
  }

  /**
   * Demonstrates async tracing with context propagation.
   *
   * <p>Endpoint: GET /api/v1/tracing/otel/async?taskId=task-123
   */
  @GetMapping("/otel/async")
  public ResponseEntity<ProcessingResult> testAsyncTracing(
      @RequestParam(defaultValue = "async-task-001") String taskId) {
    logger.info("Testing async OpenTelemetry tracing");

    try {
      CompletableFuture<String> futureResult = demoService.asyncTracingExample(taskId);
      String result = futureResult.get(); // Wait for completion
      return ResponseEntity.ok(buildResult("otel-async", result, "OpenTelemetry"));
    } catch (Exception e) {
      logger.error("Async tracing test failed", e);
      return ResponseEntity.internalServerError().body(buildError("otel-async", e));
    }
  }

  /**
   * Demonstrates custom span builder with fluent API.
   *
   * <p>Endpoint: GET /api/v1/tracing/otel/custom?operationId=op-123
   */
  @GetMapping("/otel/custom")
  public ResponseEntity<ProcessingResult> testCustomSpanBuilder(
      @RequestParam(defaultValue = "custom-op-001") String operationId) {
    logger.info("Testing custom span builder");

    try {
      String result = demoService.customSpanBuilderExample(operationId);
      return ResponseEntity.ok(buildResult("otel-custom", result, "OpenTelemetry"));
    } catch (Exception e) {
      logger.error("Custom span builder test failed", e);
      return ResponseEntity.internalServerError().body(buildError("otel-custom", e));
    }
  }

  /**
   * Demonstrates baggage propagation.
   *
   * <p>Endpoint: GET /api/v1/tracing/otel/baggage?userId=user123&tenantId=tenant456
   */
  @GetMapping("/otel/baggage")
  public ResponseEntity<ProcessingResult> testBaggage(
      @RequestParam(defaultValue = "user-001") String userId,
      @RequestParam(defaultValue = "tenant-001") String tenantId) {
    logger.info("Testing baggage propagation");

    String result = demoService.baggageExample(userId, tenantId);
    return ResponseEntity.ok(buildResult("otel-baggage", result, "OpenTelemetry"));
  }

  // ========================================================================================
  // MICROMETER DECLARATIVE TRACING ENDPOINTS
  // ========================================================================================

  /**
   * Demonstrates basic Micrometer @Observed annotation.
   *
   * <p>Endpoint: GET /api/v1/tracing/micrometer/basic?input=sample-data
   */
  @GetMapping("/micrometer/basic")
  public ResponseEntity<ProcessingResult> testBasicMicrometer(
      @RequestParam(defaultValue = "sample-input") String input) {
    logger.info("Testing basic Micrometer tracing");

    String result = demoService.basicMicrometerTracing(input);
    return ResponseEntity.ok(buildResult("micrometer-basic", result, "Micrometer"));
  }

  /**
   * Demonstrates Micrometer with low-cardinality tags.
   *
   * <p>Endpoint: GET /api/v1/tracing/micrometer/tags?operationId=op-123
   */
  @GetMapping("/micrometer/tags")
  public ResponseEntity<ProcessingResult> testMicrometerWithTags(
      @RequestParam(defaultValue = "tagged-op-001") String operationId) {
    logger.info("Testing Micrometer with tags");

    String result = demoService.micrometerWithTags(operationId);
    return ResponseEntity.ok(buildResult("micrometer-tags", result, "Micrometer"));
  }

  /**
   * Demonstrates nested Micrometer observations.
   *
   * <p>Endpoint: GET /api/v1/tracing/micrometer/nested?workflowId=wf-123
   */
  @GetMapping("/micrometer/nested")
  public ResponseEntity<ProcessingResult> testNestedMicrometer(
      @RequestParam(defaultValue = "micrometer-wf-001") String workflowId) {
    logger.info("Testing nested Micrometer observations");

    String result = demoService.nestedMicrometerObservations(workflowId);
    return ResponseEntity.ok(buildResult("micrometer-nested", result, "Micrometer"));
  }

  // ========================================================================================
  // HYBRID APPROACH ENDPOINT
  // ========================================================================================

  /**
   * Demonstrates BOTH OpenTelemetry and Micrometer working together.
   *
   * <p>This is the recommended approach for complex scenarios: - Use Micrometer for high-level
   * observations - Use OpenTelemetry for fine-grained span control
   *
   * <p>Endpoint: GET /api/v1/tracing/hybrid?operationId=hybrid-001
   */
  @GetMapping("/hybrid")
  public ResponseEntity<ProcessingResult> testHybridTracing(
      @RequestParam(defaultValue = "hybrid-op-001") String operationId) {
    logger.info("Testing hybrid tracing (OpenTelemetry + Micrometer)");

    try {
      String result = demoService.hybridTracingExample(operationId);
      return ResponseEntity.ok(buildResult("hybrid", result, "OpenTelemetry + Micrometer"));
    } catch (Exception e) {
      logger.error("Hybrid tracing test failed", e);
      return ResponseEntity.internalServerError().body(buildError("hybrid", e));
    }
  }

  // ========================================================================================
  // COMPARISON ENDPOINT
  // ========================================================================================

  /**
   * Returns comprehensive comparison between OpenTelemetry and Micrometer.
   *
   * <p>Endpoint: GET /api/v1/tracing/comparison
   */
  @GetMapping("/comparison")
  public ResponseEntity<Map<String, String>> getTracingComparison() {
    logger.info("Retrieving OpenTelemetry vs Micrometer comparison");
    return ResponseEntity.ok(demoService.getTracingComparison());
  }

  /**
   * Returns information about all available tracing demo endpoints.
   *
   * <p>Endpoint: GET /api/v1/tracing/info
   */
  @GetMapping("/info")
  public ResponseEntity<ProcessingResult> getTracingInfo() {
    logger.info("Retrieving tracing demo information");

    ProcessingResult result =
        ProcessingResult.builder()
            .resultId("tracing-info")
            .message("Distributed Tracing Demo Endpoints")
            .status("SUCCESS")
            .sourceService("tracing-demo")
            .operationType("INFO")
            .addMetadata(
                "otel_endpoints",
                "/otel/programmatic, /otel/nested, /otel/client, /otel/async, /otel/custom, /otel/baggage")
            .addMetadata(
                "micrometer_endpoints", "/micrometer/basic, /micrometer/tags, /micrometer/nested")
            .addMetadata("hybrid_endpoints", "/hybrid")
            .addMetadata("comparison_endpoint", "/comparison")
            .addMetadata(
                "recommendation",
                "Use Micrometer for standard cases, OpenTelemetry for advanced scenarios")
            .addMetadata(
                "best_practice", "Both can coexist - choose based on use case (see /comparison)")
            .build();

    return ResponseEntity.ok(result);
  }

  // ========================================================================================
  // UTILITY METHODS
  // ========================================================================================

  private ProcessingResult buildResult(String pattern, String message, String framework) {
    return ProcessingResult.builder()
        .resultId(pattern + "-" + System.currentTimeMillis())
        .message(message)
        .status("SUCCESS")
        .sourceService("tracing-demo")
        .operationType(pattern.toUpperCase().replace("-", "_"))
        .addBusinessContext("tracing.pattern", pattern)
        .addBusinessContext("tracing.framework", framework)
        .addMetadata("timestamp", System.currentTimeMillis())
        .build();
  }

  private ProcessingResult buildError(String pattern, Exception e) {
    return ProcessingResult.builder()
        .resultId(pattern + "-error-" + System.currentTimeMillis())
        .message("Tracing demo failed: " + e.getMessage())
        .status("ERROR")
        .sourceService("tracing-demo")
        .operationType(pattern.toUpperCase().replace("-", "_"))
        .addBusinessContext("tracing.pattern", pattern)
        .addBusinessContext("error.message", e.getMessage())
        .addMetadata("timestamp", System.currentTimeMillis())
        .build();
  }
}
