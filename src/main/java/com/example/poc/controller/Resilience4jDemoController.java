package com.example.poc.controller;

import com.example.poc.model.ProcessingResult;
import com.example.poc.service.Resilience4jDemoService;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller demonstrating all Resilience4j patterns and annotations.
 *
 * <p>This controller provides endpoints to test and demonstrate:
 * - Circuit Breaker (with different states and fallbacks)
 * - Retry (with exponential backoff and selective retry)
 * - Rate Limiter (with and without waiting)
 * - Bulkhead (both Semaphore and Thread Pool types)
 * - Time Limiter (for async operations)
 * - Cache (Cacheable, CachePut, CacheEvict)
 * - Combined patterns (multiple annotations on single method)
 *
 * <p>Each endpoint includes comprehensive logging and trace context propagation.
 */
@RestController
@RequestMapping("/api/v1/resilience4j")
public class Resilience4jDemoController {
  private static final Logger logger = LoggerFactory.getLogger(Resilience4jDemoController.class);

  private final Resilience4jDemoService demoService;

  public Resilience4jDemoController(Resilience4jDemoService demoService) {
    this.demoService = demoService;
  }

  // ========================================================================================
  // CIRCUIT BREAKER ENDPOINTS
  // ========================================================================================

  /**
   * Test basic circuit breaker.
   *
   * <p>Usage:
   * - GET /api/v1/resilience4j/circuit-breaker?shouldFail=false (Success)
   * - GET /api/v1/resilience4j/circuit-breaker?shouldFail=true (Triggers fallback)
   *
   * <p>Send multiple failures to open the circuit, then observe the OPEN state behavior.
   */
  @GetMapping("/circuit-breaker")
  public ResponseEntity<ProcessingResult> testCircuitBreaker(
      @RequestParam(defaultValue = "false") boolean shouldFail) {
    logger.info("Testing circuit breaker with shouldFail={}", shouldFail);

    try {
      String result = demoService.basicCircuitBreakerDemo(shouldFail);
      return ResponseEntity.ok(buildSuccessResult("circuit-breaker", result));
    } catch (Exception e) {
      logger.error("Circuit breaker test failed", e);
      return ResponseEntity.internalServerError()
          .body(buildErrorResult("circuit-breaker", e.getMessage()));
    }
  }

  /**
   * Test circuit breaker with multiple fallback methods.
   *
   * <p>Usage:
   * - GET /api/v1/resilience4j/circuit-breaker-multi?exceptionType=runtime
   * - GET /api/v1/resilience4j/circuit-breaker-multi?exceptionType=illegal
   * - GET /api/v1/resilience4j/circuit-breaker-multi?exceptionType=none
   */
  @GetMapping("/circuit-breaker-multi")
  public ResponseEntity<ProcessingResult> testCircuitBreakerMultiFallback(
      @RequestParam(defaultValue = "none") String exceptionType) {
    logger.info("Testing circuit breaker with multi-fallback, exceptionType={}", exceptionType);

    try {
      String result = demoService.circuitBreakerWithTypedFallbacks(exceptionType);
      return ResponseEntity.ok(buildSuccessResult("circuit-breaker-multi", result));
    } catch (Exception e) {
      logger.error("Multi-fallback circuit breaker test failed", e);
      return ResponseEntity.internalServerError()
          .body(buildErrorResult("circuit-breaker-multi", e.getMessage()));
    }
  }

  // ========================================================================================
  // RETRY ENDPOINTS
  // ========================================================================================

  /**
   * Test basic retry with exponential backoff.
   *
   * <p>Usage:
   * - GET /api/v1/resilience4j/retry?attemptToFail=0 (Success on first attempt)
   * - GET /api/v1/resilience4j/retry?attemptToFail=2 (Success after 2 retries)
   * - GET /api/v1/resilience4j/retry?attemptToFail=5 (Exhausts all retries, triggers fallback)
   */
  @GetMapping("/retry")
  public ResponseEntity<ProcessingResult> testRetry(
      @RequestParam(defaultValue = "0") int attemptToFail) {
    logger.info("Testing retry with attemptToFail={}", attemptToFail);

    try {
      String result = demoService.basicRetryDemo(attemptToFail);
      return ResponseEntity.ok(buildSuccessResult("retry", result));
    } catch (Exception e) {
      logger.error("Retry test failed", e);
      return ResponseEntity.internalServerError()
          .body(buildErrorResult("retry", e.getMessage()));
    }
  }

  /**
   * Test selective retry (some exceptions retried, others not).
   *
   * <p>Usage:
   * - GET /api/v1/resilience4j/retry-selective?exceptionType=retry (Retries)
   * - GET /api/v1/resilience4j/retry-selective?exceptionType=ignore (No retry)
   * - GET /api/v1/resilience4j/retry-selective?exceptionType=none (Success)
   */
  @GetMapping("/retry-selective")
  public ResponseEntity<ProcessingResult> testSelectiveRetry(
      @RequestParam(defaultValue = "none") String exceptionType) {
    logger.info("Testing selective retry with exceptionType={}", exceptionType);

    try {
      String result = demoService.selectiveRetryDemo(exceptionType);
      return ResponseEntity.ok(buildSuccessResult("retry-selective", result));
    } catch (Exception e) {
      logger.error("Selective retry test failed", e);
      return ResponseEntity.internalServerError()
          .body(buildErrorResult("retry-selective", e.getMessage()));
    }
  }

  // ========================================================================================
  // RATE LIMITER ENDPOINTS
  // ========================================================================================

  /**
   * Test basic rate limiter (fails immediately when limit exceeded).
   *
   * <p>Usage: Send 15+ requests within 1 second to trigger rate limiting
   * - curl http://localhost:8080/api/v1/resilience4j/rate-limiter?requestId=req-{1..20}
   */
  @GetMapping("/rate-limiter")
  public ResponseEntity<ProcessingResult> testRateLimiter(
      @RequestParam(defaultValue = "req-001") String requestId) {
    logger.info("Testing rate limiter for request: {}", requestId);

    try {
      String result = demoService.basicRateLimiterDemo(requestId);
      return ResponseEntity.ok(buildSuccessResult("rate-limiter", result));
    } catch (Exception e) {
      logger.warn("Rate limit exceeded for request: {}", requestId);
      return ResponseEntity.status(429)
          .body(buildErrorResult("rate-limiter", "Rate limit exceeded"));
    }
  }

  /**
   * Test rate limiter with wait timeout.
   *
   * <p>This endpoint waits up to 500ms for permission instead of failing immediately.
   */
  @GetMapping("/rate-limiter-wait")
  public ResponseEntity<ProcessingResult> testRateLimiterWithWait(
      @RequestParam(defaultValue = "req-001") String requestId) {
    logger.info("Testing rate limiter with wait for request: {}", requestId);

    try {
      String result = demoService.rateLimiterWithWaitDemo(requestId);
      return ResponseEntity.ok(buildSuccessResult("rate-limiter-wait", result));
    } catch (Exception e) {
      logger.warn("Rate limit exceeded (even after wait) for request: {}", requestId);
      return ResponseEntity.status(429)
          .body(buildErrorResult("rate-limiter-wait", "Rate limit exceeded"));
    }
  }

  // ========================================================================================
  // BULKHEAD ENDPOINTS
  // ========================================================================================

  /**
   * Test semaphore-based bulkhead.
   *
   * <p>Send 5+ concurrent requests to trigger bulkhead limits.
   */
  @GetMapping("/bulkhead-semaphore")
  public ResponseEntity<ProcessingResult> testSemaphoreBulkhead(
      @RequestParam(defaultValue = "task-001") String taskId) {
    logger.info("Testing semaphore bulkhead for task: {}", taskId);

    try {
      String result = demoService.semaphoreBulkheadDemo(taskId);
      return ResponseEntity.ok(buildSuccessResult("bulkhead-semaphore", result));
    } catch (Exception e) {
      logger.warn("Bulkhead full for task: {}", taskId);
      return ResponseEntity.status(503)
          .body(buildErrorResult("bulkhead-semaphore", "Too many concurrent requests"));
    }
  }

  /**
   * Test thread pool bulkhead (async).
   *
   * <p>Uses separate thread pool for isolation.
   */
  @GetMapping("/bulkhead-threadpool")
  public ResponseEntity<ProcessingResult> testThreadPoolBulkhead(
      @RequestParam(defaultValue = "task-001") String taskId) throws Exception {
    logger.info("Testing thread pool bulkhead for task: {}", taskId);

    try {
      CompletableFuture<String> futureResult = demoService.threadPoolBulkheadDemo(taskId);
      String result = futureResult.get(); // Wait for completion
      return ResponseEntity.ok(buildSuccessResult("bulkhead-threadpool", result));
    } catch (Exception e) {
      logger.warn("Thread pool bulkhead full for task: {}", taskId);
      return ResponseEntity.status(503)
          .body(buildErrorResult("bulkhead-threadpool", "Thread pool queue is full"));
    }
  }

  // ========================================================================================
  // TIME LIMITER ENDPOINTS
  // ========================================================================================

  /**
   * Test time limiter.
   *
   * <p>Usage:
   * - GET /api/v1/resilience4j/time-limiter?processingTimeMs=1000 (Success, under limit)
   * - GET /api/v1/resilience4j/time-limiter?processingTimeMs=5000 (Timeout, triggers fallback)
   */
  @GetMapping("/time-limiter")
  public ResponseEntity<ProcessingResult> testTimeLimiter(
      @RequestParam(defaultValue = "1000") int processingTimeMs) throws Exception {
    logger.info("Testing time limiter with processing time: {}ms", processingTimeMs);

    try {
      CompletableFuture<String> futureResult = demoService.timeLimiterDemo(processingTimeMs);
      String result = futureResult.get(); // Wait for completion
      return ResponseEntity.ok(buildSuccessResult("time-limiter", result));
    } catch (Exception e) {
      logger.warn("Time limit exceeded for operation");
      return ResponseEntity.status(504)
          .body(buildErrorResult("time-limiter", "Operation timed out"));
    }
  }

  // ========================================================================================
  // CACHE ENDPOINTS
  // ========================================================================================

  /**
   * Test cacheable operation.
   *
   * <p>First call is slow (1s), subsequent calls with same userId are instant (cached).
   */
  @GetMapping("/cache/{userId}")
  public ResponseEntity<ProcessingResult> testCacheable(@PathVariable String userId) {
    logger.info("Testing cacheable for user: {}", userId);

    long startTime = System.currentTimeMillis();
    String result = demoService.cacheableDemo(userId);
    long duration = System.currentTimeMillis() - startTime;

    ProcessingResult response = buildSuccessResult("cache", result);
    response.addMetadata("duration_ms", duration);
    response.addMetadata("cached", duration < 100); // If <100ms, probably from cache

    return ResponseEntity.ok(response);
  }

  /**
   * Test cache put (updates cache).
   */
  @PutMapping("/cache/{userId}")
  public ResponseEntity<ProcessingResult> testCachePut(
      @PathVariable String userId, @RequestParam String newData) {
    logger.info("Testing cache put for user: {}", userId);

    String result = demoService.cachePutDemo(userId, newData);
    return ResponseEntity.ok(buildSuccessResult("cache-put", result));
  }

  /**
   * Test cache evict (removes specific entry).
   */
  @DeleteMapping("/cache/{userId}")
  public ResponseEntity<ProcessingResult> testCacheEvict(@PathVariable String userId) {
    logger.info("Testing cache evict for user: {}", userId);

    demoService.cacheEvictDemo(userId);
    return ResponseEntity.ok(buildSuccessResult("cache-evict", "Cache evicted for user: " + userId));
  }

  /**
   * Test cache evict all.
   */
  @DeleteMapping("/cache")
  public ResponseEntity<ProcessingResult> testCacheEvictAll() {
    logger.info("Testing cache evict all");

    demoService.cacheEvictAllDemo();
    return ResponseEntity.ok(buildSuccessResult("cache-evict-all", "All cache entries evicted"));
  }

  // ========================================================================================
  // COMBINED PATTERNS ENDPOINTS
  // ========================================================================================

  /**
   * Test triple pattern combination (Rate Limiter + Circuit Breaker + Retry).
   *
   * <p>This is the most common production pattern for external service calls.
   */
  @GetMapping("/combined-triple")
  public ResponseEntity<ProcessingResult> testTriplePattern(
      @RequestParam(defaultValue = "op-001") String operationId) {
    logger.info("Testing triple pattern combination for operation: {}", operationId);

    try {
      String result = demoService.triplePatternDemo(operationId);
      return ResponseEntity.ok(buildSuccessResult("combined-triple", result));
    } catch (Exception e) {
      logger.error("Triple pattern test failed", e);
      return ResponseEntity.internalServerError()
          .body(buildErrorResult("combined-triple", e.getMessage()));
    }
  }

  /**
   * Test maximum resilience (ALL patterns combined).
   *
   * <p>Demonstrates: RateLimiter + CircuitBreaker + Retry + TimeLimiter + Bulkhead + Cache
   */
  @GetMapping("/combined-max")
  public ResponseEntity<ProcessingResult> testMaximumResilience(
      @RequestParam(defaultValue = "op-001") String operationId) throws Exception {
    logger.info("Testing maximum resilience for operation: {}", operationId);

    try {
      CompletableFuture<String> futureResult = demoService.maximumResilienceDemo(operationId);
      String result = futureResult.get();
      return ResponseEntity.ok(buildSuccessResult("combined-max", result));
    } catch (Exception e) {
      logger.error("Maximum resilience test failed", e);
      return ResponseEntity.status(503)
          .body(buildErrorResult("combined-max", e.getMessage()));
    }
  }

  // ========================================================================================
  // INFO ENDPOINT
  // ========================================================================================

  /**
   * Get information about all available resilience patterns and endpoints.
   */
  @GetMapping("/info")
  public ResponseEntity<ProcessingResult> getResilienceInfo() {
    logger.info("Retrieving resilience4j demo information");

    ProcessingResult result =
        ProcessingResult.builder()
            .resultId("resilience4j-info")
            .message("Resilience4j Demo Endpoints")
            .status("SUCCESS")
            .sourceService("resilience4j-demo")
            .operationType("INFO")
            .addMetadata("circuit_breaker_endpoints", "/circuit-breaker, /circuit-breaker-multi")
            .addMetadata("retry_endpoints", "/retry, /retry-selective")
            .addMetadata("rate_limiter_endpoints", "/rate-limiter, /rate-limiter-wait")
            .addMetadata("bulkhead_endpoints", "/bulkhead-semaphore, /bulkhead-threadpool")
            .addMetadata("time_limiter_endpoints", "/time-limiter")
            .addMetadata("cache_endpoints", "/cache/{userId} (GET/PUT/DELETE), /cache (DELETE)")
            .addMetadata("combined_endpoints", "/combined-triple, /combined-max")
            .addMetadata("total_patterns", 6)
            .addMetadata("documentation", "See README.md for detailed usage")
            .build();

    // Add trace context
    Span currentSpan = Span.current();
    result.addMetadata("trace_id", currentSpan.getSpanContext().getTraceId());
    result.addMetadata("span_id", currentSpan.getSpanContext().getSpanId());

    // Add baggage context
    Baggage baggage = Baggage.current();
    String productCode = baggage.getEntryValue("business.product.code");
    if (productCode != null) {
      result.addMetadata("product_code", productCode);
    }

    return ResponseEntity.ok(result);
  }

  // ========================================================================================
  // UTILITY METHODS
  // ========================================================================================

  private ProcessingResult buildSuccessResult(String pattern, String message) {
    return ProcessingResult.builder()
        .resultId(pattern + "-" + System.currentTimeMillis())
        .message(message)
        .status("SUCCESS")
        .sourceService("resilience4j-demo")
        .operationType(pattern.toUpperCase().replace("-", "_"))
        .addBusinessContext("pattern", pattern)
        .addMetadata("timestamp", System.currentTimeMillis())
        .build();
  }

  private ProcessingResult buildErrorResult(String pattern, String errorMessage) {
    return ProcessingResult.builder()
        .resultId(pattern + "-error-" + System.currentTimeMillis())
        .message("Pattern test failed: " + errorMessage)
        .status("ERROR")
        .sourceService("resilience4j-demo")
        .operationType(pattern.toUpperCase().replace("-", "_"))
        .addBusinessContext("pattern", pattern)
        .addBusinessContext("error_message", errorMessage)
        .addMetadata("timestamp", System.currentTimeMillis())
        .build();
  }
}
