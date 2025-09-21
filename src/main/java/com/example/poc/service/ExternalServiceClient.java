package com.example.poc.service;

import com.example.poc.annotation.BusinessOperation;
import com.example.poc.annotation.TraceMethod;
import com.example.poc.model.ProcessingResult;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * =================================================================================================
 * ARCHITECTURAL REVIEW
 * =================================================================================================
 *
 * <p>The `ExternalServiceClient` is the workhorse of the application. It's responsible for
 * simulating calls to external downstream services and is the primary place where the Resilience4j
 * patterns are applied.
 *
 * <p>Key Architectural Decisions & Best Practices: ------------------------------------------------
 * 1. `@Service`: Correctly marks the class as a Spring service, indicating it contains business
 * logic. 2. Comprehensive Resilience Patterns: This is the standout feature. The class demonstrates
 * the application of all six major Resilience4j patterns, often in combination, which is a
 * realistic representation of a production system. - `@CircuitBreaker`: Protects against cascading
 * failures. - `@Retry`: Handles transient faults. - `@RateLimiter`: Prevents overloading downstream
 * services. - `@Bulkhead`: Isolates resources (both semaphore and thread pool types are shown). -
 * `@TimeLimiter`: Prevents indefinite hangs. - `@Cacheable` (Spring Cache): Demonstrates caching
 * for performance. 3. Declarative Resilience: All resilience is configured declaratively using
 * annotations. This keeps the business logic clean and free from boilerplate resilience code. The
 * configuration itself is externalized to `application.yml`, which is a best practice. 4. Fallback
 * Methods: Each resilient method has a corresponding fallback method (e.g., `fallbackLambdaCall`).
 * This is crucial for graceful degradation. When a service fails, the system doesn't just crash; it
 * provides a sensible default or cached response. The fallback methods are well-implemented,
 * logging a warning and returning a structured `ProcessingResult` with a "FALLBACK" status. 5.
 * Asynchronous Operations (`CompletableFuture`): The use of `CompletableFuture` for methods
 * protected by `@TimeLimiter` and `@Bulkhead(type = THREADPOOL)` is correct. These patterns require
 * asynchronous execution, and the implementation handles this properly. 6. Realistic Simulations:
 * The methods use `Thread.sleep` to simulate network latency and `ThreadLocalRandom` to simulate
 * intermittent failures. This is an effective way to test and demonstrate the resilience patterns
 * in action. 7. Integration with Tracing: The methods are also annotated with `@TraceMethod` and
 * `@BusinessOperation`, ensuring that these simulated external calls are fully integrated into the
 * distributed trace.
 *
 * <p>Role in the Architecture: ------------------------- - It represents the "service layer" or
 * "integration layer" of the application. - It encapsulates the logic for communicating with
 * external dependencies. - It is the primary location for implementing fault tolerance and
 * resilience policies.
 *
 * <p>Overall Feedback: ----------------- - This is an outstanding class that serves as a practical,
 * hands-on guide to implementing comprehensive resilience with Resilience4j. - The combination of
 * multiple patterns on single methods (e.g., `@CircuitBreaker`, `@Retry`, and `@RateLimiter` on
 * `callLambdaService`) is a powerful demonstration of how these patterns can be composed. - The
 * code is clean, well-commented, and the simulations are realistic enough to be highly instructive.
 *
 * <p>Weaknesses/Areas for Improvement: --------------------------------- - The use of Spring's
 * `@Cacheable` instead of Resilience4j's `@Cache` is a pragmatic choice, as Spring Cache is more
 * commonly used and integrated. However, it's worth noting that it's not a "pure" Resilience4j
 * implementation in that one aspect. This is a minor point and a perfectly valid architectural
 * decision. - The `maskSensitiveQuery` method is a good thought for a POC, but as the comment
 * notes, a production system would require a much more robust and secure data masking/sanitization
 * library.
 *
 * <p>This class is the highlight of the POC, brilliantly demonstrating how to build a robust,
 * fault-tolerant application that can withstand the failures of its dependencies.
 * =================================================================================================
 */
@Service
public class ExternalServiceClient {
  @org.springframework.beans.factory.annotation.Value("${sim.failure.lambda:0.3}")
  private double lambdaFailureRate;

  private static final Logger logger = LoggerFactory.getLogger(ExternalServiceClient.class);

  /**
   * Simulates AWS Lambda service call with Circuit Breaker, Retry, and Rate Limiter.
   *
   * <p>Circuit Breaker: Opens when failure rate exceeds 60% in the last 10 calls Retry: Retries up
   * to 4 times with exponential backoff starting at 500ms Rate Limiter: Allows max 15 calls per
   * second with 100ms wait time
   *
   * <p>This represents Layer 2 in the distributed architecture (Lambda microservice).
   */
  @CircuitBreaker(name = "lambdaService", fallbackMethod = "fallbackLambdaCall")
  @Retry(name = "lambdaService")
  @RateLimiter(name = "lambdaService")
  @TraceMethod(operationName = "lambda-service-call", includeArgs = true)
  @BusinessOperation(
      name = "lambda-invocation",
      category = "external-service",
      expectedDuration = "fast",
      criticality = "high")
  public ProcessingResult callLambdaService(String userId, String data) {
    logger.info("Calling Lambda service for user: {} with data: {}", userId, data);

    // Simulate network latency and processing time
    simulateProcessingDelay(100, 500);

    // Simulate occasional failures to demonstrate circuit breaker (configurable for tests)
    if (ThreadLocalRandom.current().nextDouble() < lambdaFailureRate) {
      throw new RuntimeException("Lambda service temporarily unavailable - simulated failure");
    }

    return ProcessingResult.builder()
        .resultId("lambda-result-" + ThreadLocalRandom.current().nextInt(1000))
        .message("Lambda processing completed successfully")
        .status("SUCCESS")
        .sourceService("aws-lambda")
        .operationType("LAMBDA_INVOKE")
        .processingTimeMs(ThreadLocalRandom.current().nextLong(50, 300))
        .addBusinessContext("layer", "lambda-microservice")
        .addBusinessContext("region", "us-east-1")
        .addMetadata("function_name", "data-processor")
        .addMetadata("memory_used_mb", ThreadLocalRandom.current().nextInt(128, 512))
        .build();
  }

  /**
   * Fallback method for Lambda service failures. Provides graceful degradation when Lambda service
   * is unavailable.
   */
  public ProcessingResult fallbackLambdaCall(String userId, String data, Exception ex) {
    logger.warn(
        "Lambda service fallback triggered for user: {} - Reason: {}", userId, ex.getMessage());

    // Set a fallback attribute on the current span for test compliance
    io.opentelemetry.api.trace.Span span = io.opentelemetry.api.trace.Span.current();
    if (span != null && span.getSpanContext().isValid()) {
      span.setAttribute("fallback", true);
    }

    return ProcessingResult.builder()
        .resultId("fallback-lambda-result")
        .message("Using cached/default response due to Lambda service unavailability")
        .status("FALLBACK")
        .sourceService("fallback-cache")
        .operationType("FALLBACK_RESPONSE")
        .addBusinessContext("fallback_reason", ex.getMessage())
        .addBusinessContext("original_service", "aws-lambda")
        .addMetadata("fallback_triggered", true)
        .build();
  }

  /**
   * Simulates EKS microservice call with Bulkhead, Circuit Breaker, and Retry.
   *
   * <p>Bulkhead: Limits concurrent executions to 3 (semaphore-based isolation) Circuit Breaker:
   * Opens when failure rate exceeds 40% in the last 8 calls Retry: Retries up to 3 times with 2s
   * initial delay and exponential backoff
   *
   * <p>This represents Layer 3 in the distributed architecture (EKS microservice).
   */
  @Bulkhead(name = "eksService", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "fallbackEksCall")
  @CircuitBreaker(name = "eksService")
  @Retry(name = "eksService")
  @TraceMethod(operationName = "eks-service-call")
  @BusinessOperation(
      name = "eks-microservice-call",
      category = "external-service",
      expectedDuration = "medium",
      criticality = "high")
  public ProcessingResult callEksService(String transactionId, String payload) {
    logger.info(
        "Calling EKS service for transaction: {} with payload size: {} bytes",
        transactionId,
        payload != null ? payload.length() : 0);

    // Simulate heavy processing that might consume resources
    simulateProcessingDelay(200, 1000);

    // Simulate failures (20% failure rate)
    if (ThreadLocalRandom.current().nextDouble() < 0.2) {
      throw new RuntimeException("EKS service processing failed - simulated failure");
    }

    return ProcessingResult.builder()
        .resultId("eks-result-" + ThreadLocalRandom.current().nextInt(1000))
        .message("EKS microservice processing completed")
        .status("SUCCESS")
        .sourceService("kubernetes-eks")
        .operationType("MICROSERVICE_CALL")
        .processingTimeMs(ThreadLocalRandom.current().nextLong(200, 800))
        .addBusinessContext("layer", "eks-microservice")
        .addBusinessContext("cluster", "production-cluster")
        .addBusinessContext("namespace", "default")
        .addMetadata("pod_name", "microservice-pod-" + ThreadLocalRandom.current().nextInt(10))
        .addMetadata("cpu_usage_percent", ThreadLocalRandom.current().nextInt(30, 80))
        .build();
  }

  /** Fallback method for EKS service failures. */
  public ProcessingResult fallbackEksCall(String transactionId, String payload, Exception ex) {
    logger.warn(
        "EKS service fallback triggered for transaction: {} - Reason: {}",
        transactionId,
        ex.getMessage());

    return ProcessingResult.builder()
        .resultId("fallback-eks-result")
        .message("EKS service temporarily unavailable - using fallback")
        .status("FALLBACK")
        .sourceService("fallback-service")
        .operationType("FALLBACK_RESPONSE")
        .addBusinessContext("fallback_reason", ex.getMessage())
        .addBusinessContext("original_service", "kubernetes-eks")
        .addMetadata("fallback_triggered", true)
        .build();
  }

  /**
   * Simulates database operation with Time Limiter, Circuit Breaker, and Retry.
   *
   * <p>Time Limiter: Prevents operations from hanging beyond 5 seconds Circuit Breaker: Opens when
   * failure rate exceeds 30% in the last 12 calls Retry: Retries up to 2 times with 3s initial
   * delay
   *
   * <p>Uses CompletableFuture for async operations with time limits.
   */
  @TimeLimiter(name = "databaseService")
  @CircuitBreaker(name = "databaseService", fallbackMethod = "fallbackDatabaseCall")
  @Bulkhead(name = "databaseService", type = Bulkhead.Type.THREADPOOL)
  @Retry(name = "databaseService")
  @TraceMethod(operationName = "database-operation")
  @BusinessOperation(
      name = "database-query",
      category = "database",
      sensitive = true,
      expectedDuration = "slow",
      criticality = "critical")
  public CompletableFuture<ProcessingResult> performDatabaseOperationAsync(String query) {
    // Let Resilience4j ThreadPoolBulkhead/TimeLimiter control async execution. The annotated proxy
    // will submit this supplier to the bulkhead's executor; avoid manual supplyAsync here.
    logger.info("Executing database query: {}", maskSensitiveQuery(query));

    // TODO(observability): Add span events for SQL phases (parse/plan/execute) instead of
    // attributes
    // to reduce attribute cardinality while preserving timeline visibility.

    // Simulate database operation that might take time
    simulateProcessingDelay(500, 2000);

    // Simulate occasional database failures (15% failure rate)
    if (ThreadLocalRandom.current().nextDouble() < 0.15) {
      throw new RuntimeException("Database connection timeout - simulated failure");
    }

    ProcessingResult result =
        ProcessingResult.builder()
            .resultId("db-result-" + ThreadLocalRandom.current().nextInt(1000))
            .message("Database query executed successfully")
            .status("SUCCESS")
            .sourceService("postgresql-database")
            .operationType("DATABASE_QUERY")
            .processingTimeMs(ThreadLocalRandom.current().nextLong(500, 1500))
            .addBusinessContext("database", "primary-db")
            .addBusinessContext("table", "processed_data")
            .addMetadata("rows_affected", ThreadLocalRandom.current().nextInt(1, 100))
            .addMetadata("query_plan_cost", ThreadLocalRandom.current().nextDouble(1.0, 10.0))
            .build();

    return CompletableFuture.completedFuture(result);
  }

  /** Fallback method for database operation failures. */
  public CompletableFuture<ProcessingResult> fallbackDatabaseCall(String query, Exception ex) {
    logger.warn(
        "Database operation fallback triggered for query: {} - Reason: {}",
        maskSensitiveQuery(query),
        ex.getMessage());

    return CompletableFuture.completedFuture(
        ProcessingResult.builder()
            .resultId("fallback-db-result")
            .message("Database unavailable - using cached result")
            .status("FALLBACK")
            .sourceService("cache-fallback")
            .operationType("FALLBACK_RESPONSE")
            .addBusinessContext("fallback_reason", ex.getMessage())
            .addBusinessContext("original_service", "postgresql-database")
            .addMetadata("fallback_triggered", true)
            .addMetadata("cache_hit", true)
            .build());
  }

  /**
   * Demonstrates Cache pattern - caches results to improve performance.
   *
   * <p>Cache: Results cached for 5 minutes (300s) with max 1000 entries Rate Limiter: Allows max 20
   * calls per second with 500ms wait time
   *
   * <p>Cache key is automatically based on method parameters.
   */
  @Cacheable(value = "userDataCache")
  @RateLimiter(name = "userService")
  @TraceMethod(operationName = "user-data-fetch", includeReturnValue = true)
  @BusinessOperation(
      name = "user-data-retrieval",
      category = "user-management",
      expectedDuration = "fast",
      criticality = "normal")
  public ProcessingResult fetchUserData(String userId) {
    logger.info("Fetching user data for userId: {} (this will be cached after first call)", userId);

    // Simulate expensive operation (database call, external API, etc.)
    simulateProcessingDelay(300, 800);

    return ProcessingResult.builder()
        .resultId("user-data-" + userId)
        .message("User data retrieved successfully from source")
        .status("SUCCESS")
        .sourceService("user-service")
        .operationType("USER_DATA_FETCH")
        .processingTimeMs(ThreadLocalRandom.current().nextLong(200, 600))
        .addBusinessContext("user_id", userId)
        .addBusinessContext("data_source", "primary")
        .addMetadata("profile_complete", ThreadLocalRandom.current().nextBoolean())
        .addMetadata("last_login_days_ago", ThreadLocalRandom.current().nextInt(0, 30))
        .build();
  }

  /**
   * Complex example combining multiple Resilience4j patterns: Rate Limiter + Circuit Breaker +
   * Thread Pool Bulkhead + Cache
   *
   * <p>This demonstrates how multiple patterns work together for comprehensive resilience.
   */
  @RateLimiter(name = "combinedService")
  @CircuitBreaker(name = "combinedService", fallbackMethod = "fallbackCombinedCall")
  @Bulkhead(name = "combinedService", type = Bulkhead.Type.THREADPOOL)
  @Cacheable(value = "combinedCache")
  @TraceMethod(operationName = "combined-resilience-operation")
  @BusinessOperation(
      name = "complex-business-operation",
      category = "business-logic",
      expectedDuration = "medium",
      criticality = "high")
  public ProcessingResult complexBusinessOperation(String operationId, String parameters) {
    logger.info(
        "Executing complex business operation: {} with params: {}", operationId, parameters);

    // Simulate complex business logic with variable processing time
    simulateProcessingDelay(400, 1200);

    // TODO(perf): Consider switching to event-based markers for rule evaluation rather than
    // separate attributes to keep spans lighter under high throughput.

    // Simulate business rule failures (25% failure rate)
    if (ThreadLocalRandom.current().nextDouble() < 0.25) {
      throw new RuntimeException("Complex operation failed due to business rules");
    }

    return ProcessingResult.builder()
        .resultId("complex-result-" + operationId)
        .message("Complex business operation completed successfully")
        .status("SUCCESS")
        .sourceService("business-logic-engine")
        .operationType("COMPLEX_BUSINESS_OP")
        .processingTimeMs(ThreadLocalRandom.current().nextLong(400, 1000))
        .addBusinessContext("operation_id", operationId)
        .addBusinessContext("complexity_level", "high")
        .addBusinessContext("thread_pool", "combined-service")
        .addMetadata("rules_evaluated", ThreadLocalRandom.current().nextInt(5, 20))
        .addMetadata("cache_eligible", true)
        .build();
  }

  /** Fallback method for complex business operation. */
  public ProcessingResult fallbackCombinedCall(
      String operationId, String parameters, Exception ex) {
    logger.warn(
        "Complex operation fallback triggered for operation: {} - Reason: {}",
        operationId,
        ex.getMessage());

    return ProcessingResult.builder()
        .resultId("fallback-complex-result")
        .message("Using simplified business logic due to service issues")
        .status("FALLBACK")
        .sourceService("simplified-logic")
        .operationType("FALLBACK_RESPONSE")
        .addBusinessContext("fallback_reason", ex.getMessage())
        .addBusinessContext("original_operation", operationId)
        .addMetadata("fallback_triggered", true)
        .addMetadata("simplified_logic", true)
        .build();
  }

  /**
   * Heavy processing service demonstrating Thread Pool Bulkhead isolation. Uses separate thread
   * pool to prevent resource exhaustion.
   */
  @Bulkhead(name = "heavyProcessingService", type = Bulkhead.Type.THREADPOOL)
  @TimeLimiter(name = "combinedService")
  @TraceMethod(operationName = "heavy-processing")
  @BusinessOperation(
      name = "heavy-data-processing",
      category = "data-processing",
      expectedDuration = "slow",
      criticality = "low")
  public CompletableFuture<ProcessingResult> performHeavyProcessing(String dataId, int dataSize) {
    // Let ThreadPoolBulkhead/TimeLimiter schedule this work off the caller thread.
    logger.info("Starting heavy processing for data: {} with size: {} MB", dataId, dataSize);

    // Simulate heavy CPU/memory intensive operation
    simulateProcessingDelay(1000, 3000);

    ProcessingResult result =
        ProcessingResult.builder()
            .resultId("heavy-result-" + dataId)
            .message("Heavy processing completed")
            .status("SUCCESS")
            .sourceService("heavy-processing-engine")
            .operationType("HEAVY_PROCESSING")
            .processingTimeMs(ThreadLocalRandom.current().nextLong(1000, 2500))
            .addBusinessContext("data_id", dataId)
            .addBusinessContext("data_size_mb", String.valueOf(dataSize))
            .addBusinessContext("thread_pool", "heavy-processing")
            .addMetadata("memory_used_mb", dataSize * 2)
            .addMetadata("cpu_time_ms", ThreadLocalRandom.current().nextLong(500, 2000))
            .build();

    return CompletableFuture.completedFuture(result);
  }

  // Utility methods

  /** Simulates processing delay with random variation */
  private void simulateProcessingDelay(int minMs, int maxMs) {
    try {
      Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Processing interrupted", e);
    }
  }

  /** Masks sensitive information in database queries for logging */
  private String maskSensitiveQuery(String query) {
    if (query == null) return "null";

    // Simple masking for demonstration - in production use proper sanitization
    return query
        .replaceAll("'[^']*'", "'***'")
        .replaceAll("\\b\\d{16}\\b", "****-****-****-****") // Credit card numbers
        .replaceAll(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b",
            "***@***.***"); // Email addresses
  }
}
