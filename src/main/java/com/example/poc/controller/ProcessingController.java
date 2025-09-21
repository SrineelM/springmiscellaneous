package com.example.poc.controller;

import com.example.poc.annotation.ActionType;
import com.example.poc.annotation.BusinessOperation;
import com.example.poc.annotation.CorrelationId;
import com.example.poc.annotation.FeatureName;
import com.example.poc.model.ProcessingRequest;
import com.example.poc.model.ProcessingResult;
import com.example.poc.service.ExternalServiceClient;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * =================================================================================================
 * ARCHITECTURAL REVIEW
 * =================================================================================================
 *
 * <p>The `ProcessingController` is the main entry point for all incoming HTTP requests. It serves
 * as the "presentation layer" of this POC and is responsible for orchestrating calls to the service
 * layer and demonstrating the complete distributed tracing and resilience flow.
 *
 * <p>Key Architectural Decisions & Best Practices: ------------------------------------------------
 * 1. RESTful Design: The controller follows REST principles, using standard HTTP verbs
 * (`@PostMapping`, `@GetMapping`) and clear, hierarchical URL paths (`/api/v1/processing/...`). 2.
 * `@RestController`: Correctly combines `@Controller` and `@ResponseBody`, simplifying the code by
 * automatically serializing return objects (like `ProcessingResult`) into JSON. 3. Dependency
 * Injection: The `ExternalServiceClient` is properly injected via the constructor, which is the
 * recommended approach for mandatory dependencies. 4. Declarative Business Context: This is a major
 * strength. The controller methods are heavily annotated with the custom business annotations
 * (`@FeatureName`, `@ActionType`, `@BusinessOperation`, `@CorrelationId`). This makes the business
 * intent of each endpoint immediately clear and allows the `DistributedTracingAspect` to work its
 * magic without any imperative code in the controller. 5. Orchestration, Not Logic: The
 * `completeProcessingFlow` method is a great example of the "Orchestrator" pattern. It doesn't
 * contain any complex business logic itself; instead, it sequences and coordinates calls to the
 * `ExternalServiceClient`, which encapsulates the actual work. This separation of concerns is
 * excellent. 6. Comprehensive Endpoints: The controller provides a rich set of endpoints for
 * demonstrating different scenarios: - `complete-flow`: The main showcase of the entire
 * architecture. - `user-data`: To test caching and rate limiting. - `heavy-processing`: To test
 * thread pool bulkhead isolation. - `test/{pattern}`: A very useful endpoint for developers to test
 * each resilience pattern in isolation. - `health` & `info`: Standard endpoints for monitoring and
 * system information. 7. Asynchronous Handling: The controller correctly handles
 * `CompletableFuture` returned by the service layer by calling `.get()`. This ensures that the HTTP
 * response is not sent until the asynchronous operation (which is subject to a `TimeLimiter`) is
 * complete. 8. Rich Response Objects: The use of a `ProcessingResult` builder to create detailed,
 * structured JSON responses is a best practice. It provides the client with a wealth of information
 * about the processing outcome, including status, timing, and business context.
 *
 * <p>Role in the Architecture: ------------------------- - It's the public-facing API of the
 * service. - It's the first point of contact for incoming requests and therefore the place where
 * the tracing context (like correlation ID and user ID) is often initiated or extracted from HTTP
 * headers. - It orchestrates the business flow by delegating to service-layer components.
 *
 * <p>Overall Feedback: ----------------- - This is a well-designed, robust, and highly
 * demonstrative controller. It effectively showcases all the key features of the POC. - The use of
 * declarative annotations for cross-cutting concerns is exemplary. - The logging is thorough and
 * provides good visibility into the execution flow, with clear references to business context IDs.
 * - The error handling is solid, with `try-catch` blocks that create structured error responses.
 *
 * <p>This controller is a strong piece of the architecture, effectively bridging the gap between
 * the external world (HTTP) and the internal business logic of the application.
 * =================================================================================================
 */
@RestController
@RequestMapping("/api/v1/processing")
@FeatureName("request-processing")
public class ProcessingController {

  private static final Logger logger = LoggerFactory.getLogger(ProcessingController.class);
  private final ExternalServiceClient externalServiceClient;

  public ProcessingController(ExternalServiceClient externalServiceClient) {
    this.externalServiceClient = externalServiceClient;
  }

  /**
   * Main endpoint demonstrating complete distributed tracing flow.
   *
   * <p>This endpoint simulates the complete architecture: 1. Controller receives request (Layer 1 -
   * On-premises monolith) 2. Calls Lambda service (Layer 2 - AWS Lambda Python microservice) 3.
   * Calls EKS service (Layer 3 - EKS Java Spring microservice) 4. Performs database operation (Data
   * layer) 5. Executes complex business logic (Business layer)
   *
   * <p>All with comprehensive tracing, resilience patterns, and business context.
   */
  @PostMapping("/complete-flow")
  @ActionType("COMPLETE_PROCESSING")
  @CorrelationId(generate = true, headerName = "X-Correlation-ID")
  @BusinessOperation(
      name = "complete-processing-flow",
      category = "request-processing",
      expectedDuration = "slow",
      criticality = "high")
  public ResponseEntity<ProcessingResult> completeProcessingFlow(
      @RequestHeader(value = "User-ID", defaultValue = "anonymous") String userId,
      @RequestHeader(value = "Session-ID", required = false) String sessionId,
      @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
      @RequestBody ProcessingRequest request) {

    logger.info(
        "Starting complete processing flow for user: {} with request: {} (correlationId: {})",
        userId,
        request.getData(),
        correlationId);

    // Extract current baggage context (populated by aspect)
    Baggage currentBaggage = Baggage.current();
    String businessTransactionId = currentBaggage.getEntryValue("business.transaction.id");
    String businessCorrelationId = currentBaggage.getEntryValue("business.correlation.id");

    logger.info(
        "Processing with business context - TransactionId: {}, CorrelationId: {}, ProductCode: {}",
        businessTransactionId,
        businessCorrelationId,
        currentBaggage.getEntryValue("business.product.code"));

    try {
      long startTime = System.currentTimeMillis();

      // Step 1: Call Lambda service (Circuit Breaker, Retry, Rate Limiter)
      logger.debug("Step 1: Calling Lambda service");
      ProcessingResult lambdaResult =
          externalServiceClient.callLambdaService(userId, request.getData());
      logger.info(
          "Lambda service completed: {} (resultId: {})",
          lambdaResult.getMessage(),
          lambdaResult.getResultId());

      // Step 2: Call EKS service (Bulkhead, Circuit Breaker, Retry)
      logger.debug("Step 2: Calling EKS service");
      ProcessingResult eksResult =
          externalServiceClient.callEksService(businessTransactionId, lambdaResult.getResultId());
      logger.info(
          "EKS service completed: {} (resultId: {})",
          eksResult.getMessage(),
          eksResult.getResultId());

      // Step 3: Async database operation (Time Limiter, Circuit Breaker, Retry)
      logger.debug("Step 3: Performing database operation");
      CompletableFuture<ProcessingResult> dbOperation =
          externalServiceClient.performDatabaseOperationAsync(
              "SELECT * FROM processed_data WHERE transaction_id = '"
                  + businessTransactionId
                  + "'");
      ProcessingResult dbResult = dbOperation.get(); // This respects TimeLimiter configuration
      logger.info(
          "Database operation completed: {} (resultId: {})",
          dbResult.getMessage(),
          dbResult.getResultId());

      // Step 4: Complex business operation (multiple resilience patterns)
      logger.debug("Step 4: Executing complex business operation");
      ProcessingResult complexResult =
          externalServiceClient.complexBusinessOperation(
              request.getOperationId(), request.getParameters());
      logger.info(
          "Complex operation completed: {} (resultId: {})",
          complexResult.getMessage(),
          complexResult.getResultId());

      // Calculate total processing time
      long totalDuration = System.currentTimeMillis() - startTime;

      // Combine all results into final response
      ProcessingResult finalResult =
          ProcessingResult.builder()
              .resultId("final-" + System.currentTimeMillis())
              .message(
                  String.format(
                      "Complete flow finished successfully - Lambda: %s, EKS: %s, DB: %s, Complex: %s",
                      lambdaResult.getResultId(),
                      eksResult.getResultId(),
                      dbResult.getResultId(),
                      complexResult.getResultId()))
              .status("SUCCESS")
              .sourceService("distributed-tracing-poc")
              .operationType("COMPLETE_FLOW")
              .processingTimeMs(totalDuration)
              .addBusinessContext("user_id", userId)
              .addBusinessContext("session_id", sessionId)
              .addBusinessContext("transaction_id", businessTransactionId)
              .addBusinessContext("correlation_id", businessCorrelationId)
              .addBusinessContext("flow_type", "complete")
              .addMetadata("lambda_result_id", lambdaResult.getResultId())
              .addMetadata("eks_result_id", eksResult.getResultId())
              .addMetadata("db_result_id", dbResult.getResultId())
              .addMetadata("complex_result_id", complexResult.getResultId())
              .addMetadata("steps_completed", 4)
              .addMetadata("total_duration_ms", totalDuration)
              .build();

      // Add current span information for correlation
      Span currentSpan = Span.current();
      finalResult.addMetadata("trace_id", currentSpan.getSpanContext().getTraceId());
      finalResult.addMetadata("span_id", currentSpan.getSpanContext().getSpanId());

      logger.info(
          "Complete processing flow finished successfully for user: {} in {}ms - TxnId: {}",
          userId,
          totalDuration,
          businessTransactionId);

      return ResponseEntity.ok(finalResult);

    } catch (ExecutionException | InterruptedException e) {
      logger.error(
          "Complete processing flow failed for user: {} - Error: {}", userId, e.getMessage(), e);

      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      ProcessingResult errorResult =
          ProcessingResult.builder()
              .resultId("error-" + System.currentTimeMillis())
              .message("Processing failed: " + e.getMessage())
              .status("ERROR")
              .sourceService("distributed-tracing-poc")
              .operationType("COMPLETE_FLOW")
              .addBusinessContext("user_id", userId)
              .addBusinessContext("transaction_id", businessTransactionId)
              .addBusinessContext("error_type", e.getClass().getSimpleName())
              .addMetadata("error_message", e.getMessage())
              .addMetadata("failed_step", "unknown")
              .build();

      return ResponseEntity.internalServerError().body(errorResult);
    }
  }

  /**
   * Endpoint demonstrating caching behavior with Rate Limiter. First call hits the service,
   * subsequent calls use cache.
   */
  @GetMapping("/user-data/{userId}")
  @ActionType("USER_DATA_FETCH")
  @BusinessOperation(
      name = "user-data-retrieval",
      category = "user-management",
      expectedDuration = "fast",
      criticality = "normal")
  public ResponseEntity<ProcessingResult> getUserData(
      @PathVariable String userId,
      @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

    logger.info("Fetching user data for userId: {} (correlationId: {})", userId, correlationId);

    try {
      // This call will be cached after first execution (300s cache duration)
      ProcessingResult result = externalServiceClient.fetchUserData(userId);

      // Add correlation metadata
      result.addMetadata("correlation_id", correlationId);
      result.addMetadata("cache_eligible", true);

      logger.info(
          "User data fetch completed for userId: {} (cached: {})",
          userId,
          result.getMetadata().getOrDefault("cache_hit", false));

      return ResponseEntity.ok(result);

    } catch (Exception e) {
      logger.error("User data fetch failed for userId: {} - Error: {}", userId, e.getMessage(), e);

      ProcessingResult errorResult =
          ProcessingResult.builder()
              .resultId("error-user-data")
              .message("Failed to fetch user data: " + e.getMessage())
              .status("ERROR")
              .sourceService("user-service")
              .operationType("USER_DATA_FETCH")
              .addBusinessContext("user_id", userId)
              .addMetadata("error_type", e.getClass().getSimpleName())
              .build();

      return ResponseEntity.internalServerError().body(errorResult);
    }
  }

  /**
   * Endpoint demonstrating heavy processing with Thread Pool Bulkhead isolation. Uses separate
   * thread pool to prevent resource exhaustion.
   */
  @PostMapping("/heavy-processing")
  @ActionType("HEAVY_PROCESSING")
  @BusinessOperation(
      name = "heavy-data-processing",
      category = "data-processing",
      expectedDuration = "slow",
      criticality = "low")
  public ResponseEntity<ProcessingResult> performHeavyProcessing(
      @RequestParam String dataId,
      @RequestParam(defaultValue = "100") int dataSizeMb,
      @RequestHeader(value = "User-ID", defaultValue = "system") String userId) {

    logger.info(
        "Starting heavy processing for data: {} with size: {} MB (user: {})",
        dataId,
        dataSizeMb,
        userId);

    try {
      // This uses Thread Pool Bulkhead to isolate heavy operations
      CompletableFuture<ProcessingResult> heavyResult =
          externalServiceClient.performHeavyProcessing(dataId, dataSizeMb);
      ProcessingResult result = heavyResult.get(); // Respects TimeLimiter configuration

      // Add request context
      result.addBusinessContext("requested_by", userId);
      result.addMetadata("data_id", dataId);
      result.addMetadata("data_size_mb", dataSizeMb);

      logger.info(
          "Heavy processing completed for data: {} in {}ms", dataId, result.getProcessingTimeMs());

      return ResponseEntity.ok(result);

    } catch (Exception e) {
      logger.error("Heavy processing failed for data: {} - Error: {}", dataId, e.getMessage(), e);

      ProcessingResult errorResult =
          ProcessingResult.builder()
              .resultId("error-heavy-processing")
              .message("Heavy processing failed: " + e.getMessage())
              .status("ERROR")
              .sourceService("heavy-processing-engine")
              .operationType("HEAVY_PROCESSING")
              .addBusinessContext("data_id", dataId)
              .addBusinessContext("requested_by", userId)
              .addMetadata("error_type", e.getClass().getSimpleName())
              .addMetadata("data_size_mb", dataSizeMb)
              .build();

      return ResponseEntity.internalServerError().body(errorResult);
    }
  }

  /**
   * Endpoint to test individual resilience patterns. Allows testing specific patterns in isolation.
   */
  @GetMapping("/test/{pattern}")
  @ActionType("PATTERN_TEST")
  @BusinessOperation(
      name = "resilience-pattern-test",
      category = "testing",
      expectedDuration = "fast",
      criticality = "low")
  public ResponseEntity<ProcessingResult> testResiliencePattern(
      @PathVariable String pattern,
      @RequestParam(defaultValue = "test-user") String userId,
      @RequestParam(defaultValue = "sample-data") String data) {

    logger.info("Testing resilience pattern: {} for user: {}", pattern, userId);

    try {
      ProcessingResult result;

      switch (pattern.toLowerCase()) {
        case "circuit-breaker":
        case "cb":
          // Test Lambda service circuit breaker
          result = externalServiceClient.callLambdaService(userId, data);
          break;

        case "retry":
          // Test EKS service retry mechanism
          result =
              externalServiceClient.callEksService("test-txn-" + System.currentTimeMillis(), data);
          break;

        case "rate-limiter":
        case "rl":
          // Test user service rate limiter
          result = externalServiceClient.fetchUserData(userId);
          break;

        case "bulkhead":
          // Test EKS service bulkhead
          result =
              externalServiceClient.callEksService(
                  "bulkhead-test-" + System.currentTimeMillis(), data);
          break;

        case "time-limiter":
        case "tl":
          // Test database time limiter
          CompletableFuture<ProcessingResult> dbResult =
              externalServiceClient.performDatabaseOperationAsync(
                  "SELECT * FROM test_table WHERE id = '" + userId + "'");
          result = dbResult.get();
          break;

        case "cache":
          // Test cache pattern
          result = externalServiceClient.fetchUserData(userId);
          break;

        case "combined":
          // Test combined patterns
          result = externalServiceClient.complexBusinessOperation("test-operation", data);
          break;

        default:
          throw new IllegalArgumentException(
              "Unknown pattern: "
                  + pattern
                  + ". Available patterns: circuit-breaker, retry, rate-limiter, bulkhead, time-limiter, cache, combined");
      }

      // Add test metadata
      result.addBusinessContext("test_pattern", pattern);
      result.addBusinessContext("test_user", userId);
      result.addMetadata("test_mode", true);
      result.addMetadata("pattern_tested", pattern);

      logger.info("Pattern test completed: {} - Result: {}", pattern, result.getStatus());

      return ResponseEntity.ok(result);

    } catch (Exception e) {
      logger.error("Pattern test failed: {} - Error: {}", pattern, e.getMessage(), e);

      ProcessingResult errorResult =
          ProcessingResult.builder()
              .resultId("error-pattern-test")
              .message("Pattern test failed: " + e.getMessage())
              .status("ERROR")
              .sourceService("pattern-test")
              .operationType("PATTERN_TEST")
              .addBusinessContext("pattern", pattern)
              .addBusinessContext("test_user", userId)
              .addMetadata("error_type", e.getClass().getSimpleName())
              .addMetadata("test_mode", true)
              .build();

      return ResponseEntity.internalServerError().body(errorResult);
    }
  }

  /** Health check endpoint demonstrating basic tracing without resilience patterns. */
  @GetMapping("/health")
  @ActionType("HEALTH_CHECK")
  @BusinessOperation(
      name = "health-check",
      category = "system",
      expectedDuration = "fast",
      criticality = "normal")
  public ResponseEntity<ProcessingResult> healthCheck() {
    logger.info("Performing health check");

    // Get current trace and baggage information
    Span currentSpan = Span.current();
    Baggage currentBaggage = Baggage.current();

    ProcessingResult healthResult =
        ProcessingResult.builder()
            .resultId("health-ok-" + System.currentTimeMillis())
            .message("Service is healthy and all resilience patterns are active")
            .status("SUCCESS")
            .sourceService("distributed-tracing-poc")
            .operationType("HEALTH_CHECK")
            .addBusinessContext(
                "product_code", currentBaggage.getEntryValue("business.product.code"))
            .addBusinessContext("health_status", "healthy")
            .addMetadata("trace_id", currentSpan.getSpanContext().getTraceId())
            .addMetadata("span_id", currentSpan.getSpanContext().getSpanId())
            .addMetadata(
                "resilience_patterns",
                "circuit-breaker,retry,rate-limiter,bulkhead,time-limiter,cache")
            .addMetadata("tracing_enabled", true)
            .addMetadata("baggage_propagation", true)
            .build();

    logger.info("Health check completed successfully");

    return ResponseEntity.ok(healthResult);
  }

  /** Info endpoint providing system information and configuration details. */
  @GetMapping("/info")
  @ActionType("SYSTEM_INFO")
  @BusinessOperation(
      name = "system-info",
      category = "system",
      expectedDuration = "fast",
      criticality = "low")
  public ResponseEntity<ProcessingResult> getSystemInfo() {
    logger.info("Retrieving system information");

    ProcessingResult infoResult =
        ProcessingResult.builder()
            .resultId("system-info-" + System.currentTimeMillis())
            .message("System information retrieved successfully")
            .status("SUCCESS")
            .sourceService("distributed-tracing-poc")
            .operationType("SYSTEM_INFO")
            .addBusinessContext("application", "distributed-tracing-resilience-poc")
            .addBusinessContext("version", "1.0.0")
            .addBusinessContext("profile", System.getProperty("spring.profiles.active", "default"))
            .addMetadata("java_version", System.getProperty("java.version"))
            .addMetadata(
                "spring_boot_version", org.springframework.boot.SpringBootVersion.getVersion())
            .addMetadata("available_processors", Runtime.getRuntime().availableProcessors())
            .addMetadata("max_memory_mb", Runtime.getRuntime().maxMemory() / 1024 / 1024)
            .addMetadata("total_memory_mb", Runtime.getRuntime().totalMemory() / 1024 / 1024)
            .addMetadata("free_memory_mb", Runtime.getRuntime().freeMemory() / 1024 / 1024)
            .build();

    return ResponseEntity.ok(infoResult);
  }
}
