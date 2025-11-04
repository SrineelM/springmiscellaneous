package com.example.poc.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Comprehensive demonstration of ALL Resilience4j annotations and configuration options.
 *
 * <p>This service showcases: 1. @CircuitBreaker - All states (CLOSED, OPEN, HALF_OPEN), fallbacks,
 * event listeners 2. @Retry - Exponential backoff, max attempts, exception filtering
 * 3. @RateLimiter - Request throttling, timeout handling 4. @Bulkhead - Both SEMAPHORE and
 * THREADPOOL types 5. @TimeLimiter - Async operation timeouts 6. @Cacheable/@CachePut/@CacheEvict -
 * Caching patterns 7. Pattern combinations - Multiple annotations on single method
 *
 * <p>Each method demonstrates different aspects and configurations of the patterns.
 */
@Service
public class Resilience4jDemoService {
  private static final Logger logger = LoggerFactory.getLogger(Resilience4jDemoService.class);

  // ========================================================================================
  // CIRCUIT BREAKER DEMONSTRATIONS
  // ========================================================================================

  /**
   * Basic Circuit Breaker with fallback.
   *
   * <p>Configuration in application.yml: - failure-rate-threshold: 50% - sliding-window-size: 10
   * calls - wait-duration-in-open-state: 10s
   *
   * <p>States: CLOSED → OPEN (on threshold) → HALF_OPEN → CLOSED/OPEN
   */
  @CircuitBreaker(name = "basicCircuitBreaker", fallbackMethod = "basicFallback")
  public String basicCircuitBreakerDemo(boolean shouldFail) {
    logger.info("Executing basicCircuitBreakerDemo with shouldFail={}", shouldFail);
    if (shouldFail) {
      throw new RuntimeException("Simulated failure for circuit breaker demo");
    }
    return "Success: Circuit breaker is CLOSED";
  }

  private String basicFallback(boolean shouldFail, Throwable t) {
    logger.warn("Circuit breaker fallback triggered: {}", t.getMessage());
    return "Fallback: Circuit is OPEN - " + t.getMessage();
  }

  /**
   * Circuit Breaker with multiple fallback methods (type-specific).
   *
   * <p>Demonstrates fallback method selection based on exception type.
   */
  @CircuitBreaker(
      name = "multiTypeCB",
      fallbackMethod = "handleRuntimeException,handleGeneralException")
  public String circuitBreakerWithTypedFallbacks(String exceptionType) {
    logger.info("Testing circuit breaker with exception type: {}", exceptionType);

    switch (exceptionType) {
      case "runtime":
        throw new RuntimeException("Runtime exception triggered");
      case "illegal":
        throw new IllegalArgumentException("Illegal argument exception triggered");
      default:
        return "Success without exception";
    }
  }

  private String handleRuntimeException(String exceptionType, RuntimeException e) {
    logger.warn("RuntimeException-specific fallback: {}", e.getMessage());
    return "Fallback for RuntimeException: " + e.getMessage();
  }

  private String handleGeneralException(String exceptionType, Throwable t) {
    logger.warn("General fallback: {}", t.getMessage());
    return "General fallback: " + t.getMessage();
  }

  // ========================================================================================
  // RETRY DEMONSTRATIONS
  // ========================================================================================

  /**
   * Basic Retry with exponential backoff.
   *
   * <p>Configuration: - max-attempts: 3 - wait-duration: 1s - exponential-backoff-multiplier: 2 -
   * Pattern: 1s → 2s → 4s
   */
  @Retry(name = "basicRetry", fallbackMethod = "retryFallback")
  public String basicRetryDemo(int attemptToFail) {
    int currentAttempt = getCurrentRetryAttempt();
    logger.info("Retry attempt #{}", currentAttempt);

    if (currentAttempt <= attemptToFail) {
      throw new RuntimeException("Attempt " + currentAttempt + " failed (will retry)");
    }

    return "Success after " + currentAttempt + " attempts";
  }

  private String retryFallback(int attemptToFail, Throwable t) {
    logger.warn("All retry attempts exhausted: {}", t.getMessage());
    return "Retry fallback: All attempts failed - " + t.getMessage();
  }

  /**
   * Retry with selective exception handling.
   *
   * <p>Configuration includes: - retry-exceptions: Specific exceptions to retry -
   * ignore-exceptions: Exceptions that shouldn't trigger retry
   */
  @Retry(name = "selectiveRetry")
  public String selectiveRetryDemo(String exceptionType) {
    logger.info("Selective retry with exception: {}", exceptionType);

    switch (exceptionType) {
      case "retry":
        // This will be retried (configured in retry-exceptions)
        throw new RuntimeException("Retryable exception");
      case "ignore":
        // This will NOT be retried (configured in ignore-exceptions)
        throw new IllegalArgumentException("Non-retryable exception");
      default:
        return "Success without exception";
    }
  }

  // ========================================================================================
  // RATE LIMITER DEMONSTRATIONS
  // ========================================================================================

  /**
   * Basic Rate Limiter.
   *
   * <p>Configuration: - limit-for-period: 10 requests - limit-refresh-period: 1 second -
   * timeout-duration: 0s (fail immediately if limit exceeded)
   */
  @RateLimiter(name = "basicRateLimiter", fallbackMethod = "rateLimiterFallback")
  public String basicRateLimiterDemo(String requestId) {
    logger.info("Processing request: {}", requestId);
    return "Request " + requestId + " processed successfully";
  }

  private String rateLimiterFallback(String requestId, Throwable t) {
    logger.warn("Rate limit exceeded for request: {}", requestId);
    return "Rate limit exceeded: Please try again later";
  }

  /**
   * Rate Limiter with wait timeout.
   *
   * <p>Waits up to 500ms for permission instead of failing immediately.
   */
  @RateLimiter(name = "waitingRateLimiter")
  public String rateLimiterWithWaitDemo(String requestId) {
    logger.info("Processing request with wait: {}", requestId);
    simulateProcessing(100);
    return "Request " + requestId + " processed after waiting for permission";
  }

  // ========================================================================================
  // BULKHEAD DEMONSTRATIONS
  // ========================================================================================

  /**
   * Semaphore-based Bulkhead.
   *
   * <p>Configuration: - max-concurrent-calls: 3 - max-wait-duration: 1s
   *
   * <p>Limits concurrent executions using semaphore (doesn't use separate thread pool).
   */
  @Bulkhead(
      name = "semaphoreBulkhead",
      type = Bulkhead.Type.SEMAPHORE,
      fallbackMethod = "bulkheadFallback")
  public String semaphoreBulkheadDemo(String taskId) {
    logger.info("Executing task in semaphore bulkhead: {}", taskId);
    simulateProcessing(2000); // 2 seconds
    return "Task " + taskId + " completed in semaphore bulkhead";
  }

  private String bulkheadFallback(String taskId, Throwable t) {
    logger.warn("Bulkhead full for task: {}", taskId);
    return "Bulkhead fallback: Too many concurrent requests";
  }

  /**
   * Thread Pool Bulkhead (for async operations).
   *
   * <p>Configuration: - max-thread-pool-size: 4 - core-thread-pool-size: 2 - queue-capacity: 10
   *
   * <p>Uses separate thread pool for isolation.
   */
  @Bulkhead(
      name = "threadPoolBulkhead",
      type = Bulkhead.Type.THREADPOOL,
      fallbackMethod = "threadPoolBulkheadFallback")
  public CompletableFuture<String> threadPoolBulkheadDemo(String taskId) {
    logger.info("Executing task in thread pool bulkhead: {}", taskId);
    simulateProcessing(1000);
    String result = "Task " + taskId + " completed in thread pool bulkhead";
    return CompletableFuture.completedFuture(result);
  }

  private CompletableFuture<String> threadPoolBulkheadFallback(String taskId, Throwable t) {
    logger.warn("Thread pool bulkhead full for task: {}", taskId);
    return CompletableFuture.completedFuture("Thread pool fallback: Queue is full");
  }

  // ========================================================================================
  // TIME LIMITER DEMONSTRATIONS
  // ========================================================================================

  /**
   * Time Limiter for async operations.
   *
   * <p>Configuration: - timeout-duration: 3s - cancel-running-future: true
   *
   * <p>Cancels operation if it exceeds timeout.
   */
  @TimeLimiter(name = "basicTimeLimiter", fallbackMethod = "timeLimiterFallback")
  public CompletableFuture<String> timeLimiterDemo(int processingTimeMs) {
    logger.info("Starting time-limited operation with {}ms processing time", processingTimeMs);
    simulateProcessing(processingTimeMs);
    String result = "Operation completed in " + processingTimeMs + "ms";
    return CompletableFuture.completedFuture(result);
  }

  private CompletableFuture<String> timeLimiterFallback(int processingTimeMs, Throwable t) {
    logger.warn("Time limit exceeded for operation that should take {}ms", processingTimeMs);
    return CompletableFuture.completedFuture("Timeout fallback: Operation took too long");
  }

  // ========================================================================================
  // CACHE DEMONSTRATIONS
  // ========================================================================================

  /**
   * Basic cache - stores result for future calls.
   *
   * <p>Configuration: - expire-after-write: 300s (5 minutes) - maximum-size: 1000
   */
  @Cacheable(value = "demoCache", key = "#userId")
  public String cacheableDemo(String userId) {
    logger.info("Cache MISS - Fetching data for user: {}", userId);
    simulateProcessing(1000); // Expensive operation
    return "User data for " + userId + " (fetched at " + System.currentTimeMillis() + ")";
  }

  /** Cache Put - updates cache after execution. */
  @CachePut(value = "demoCache", key = "#userId")
  public String cachePutDemo(String userId, String newData) {
    logger.info("Updating cache for user: {}", userId);
    return "Updated data for " + userId + ": " + newData;
  }

  /** Cache Evict - removes entries from cache. */
  @CacheEvict(value = "demoCache", key = "#userId")
  public void cacheEvictDemo(String userId) {
    logger.info("Evicting cache for user: {}", userId);
  }

  /** Cache Evict All - clears entire cache. */
  @CacheEvict(value = "demoCache", allEntries = true)
  public void cacheEvictAllDemo() {
    logger.info("Evicting all cache entries");
  }

  // ========================================================================================
  // COMBINED PATTERNS DEMONSTRATIONS
  // ========================================================================================

  /**
   * Combination: Circuit Breaker + Retry + Rate Limiter.
   *
   * <p>Order of execution (outside-in): 1. RateLimiter - Check if request is allowed 2.
   * CircuitBreaker - Check circuit state 3. Retry - Execute with retry logic
   *
   * <p>This is the most common production pattern for external service calls.
   */
  @RateLimiter(name = "combinedPattern")
  @CircuitBreaker(name = "combinedPattern", fallbackMethod = "combinedFallback")
  @Retry(name = "combinedPattern")
  public String triplePatternDemo(String operationId) {
    logger.info("Executing triple pattern demo: {}", operationId);

    // Simulate occasional failures
    if (ThreadLocalRandom.current().nextDouble() < 0.3) {
      throw new RuntimeException("Random failure in triple pattern");
    }

    return "Triple pattern success: " + operationId;
  }

  private String combinedFallback(String operationId, Throwable t) {
    logger.warn("Combined pattern fallback for operation: {}", operationId);
    return "Combined fallback: All resilience patterns triggered";
  }

  /**
   * Combination: All patterns together (maximum resilience).
   *
   * <p>Demonstrates: - RateLimiter: Prevent overload - CircuitBreaker: Fast fail when service is
   * down - Retry: Handle transient failures - TimeLimiter: Prevent hanging - Bulkhead: Isolate
   * resources - Cache: Reduce load on downstream
   */
  @RateLimiter(name = "maxResilience")
  @CircuitBreaker(name = "maxResilience", fallbackMethod = "maxResilienceFallback")
  @Retry(name = "maxResilience")
  @TimeLimiter(name = "maxResilience")
  @Bulkhead(name = "maxResilience", type = Bulkhead.Type.THREADPOOL)
  @Cacheable(value = "maxResilienceCache", key = "#operationId")
  public CompletableFuture<String> maximumResilienceDemo(String operationId) {
    logger.info("Executing maximum resilience demo: {}", operationId);

    simulateProcessing(500);

    // Simulate occasional failures
    if (ThreadLocalRandom.current().nextDouble() < 0.2) {
      throw new RuntimeException("Random failure in max resilience pattern");
    }

    String result =
        "Maximum resilience success: " + operationId + " at " + System.currentTimeMillis();
    return CompletableFuture.completedFuture(result);
  }

  private CompletableFuture<String> maxResilienceFallback(String operationId, Throwable t) {
    logger.warn("Maximum resilience fallback for operation: {}", operationId);
    return CompletableFuture.completedFuture(
        "Max resilience fallback: Using cached/default response");
  }

  // ========================================================================================
  // UTILITY METHODS
  // ========================================================================================

  private void simulateProcessing(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Processing interrupted", e);
    }
  }

  private int getCurrentRetryAttempt() {
    // In real implementation, this would come from Resilience4j context
    // For demo purposes, we'll use a simplified approach
    return ThreadLocalRandom.current().nextInt(1, 4);
  }
}
