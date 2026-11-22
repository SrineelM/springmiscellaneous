package com.example.poc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.poc.PocTest;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validates state transitions and fault-tolerance mechanics for all Resilience4j patterns.
 *
 * <p>Educational value:
 * 1. CircuitBreaker transitions: CLOSED (normal) -> OPEN (tripped by failure threshold) -> HALF_OPEN (trial calls).
 * 2. RateLimiter rejection: Throws RequestNotPermitted when throughput limit is exhausted.
 * 3. Bulkhead isolation: Throws BulkheadFullException when concurrency capacity is saturated.
 * 4. TimeLimiter bound: Cancels and completes exceptionally with TimeoutException if duration threshold is exceeded.
 * 5. Retry mechanics: Repeats invocation up to max-attempts before failing.
 * 6. K1 Typed Fallbacks: Matches the most specific overloaded method parameter type.
 */
@PocTest
class Resilience4jPatternTransitionsTest {

  @Autowired private Resilience4jDemoService resilienceService;
  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;
  @Autowired private RetryRegistry retryRegistry;
  @Autowired private RateLimiterRegistry rateLimiterRegistry;
  @Autowired private BulkheadRegistry bulkheadRegistry;
  @Autowired private TimeLimiterRegistry timeLimiterRegistry;

  @Test
  @DisplayName("K1: Overloaded typed fallback routes RuntimeException to specific overload")
  void circuitBreaker_routesToTypedFallbackOverload() {
    // When a RuntimeException is thrown, typedFallback(String, RuntimeException) is chosen
    String runtimeResult = resilienceService.circuitBreakerWithTypedFallbacks("runtime");
    assertThat(runtimeResult).contains("Fallback for RuntimeException");

    // When a non-RuntimeException (Throwable) is thrown, typedFallback(String, Throwable) is chosen
    String generalResult = resilienceService.circuitBreakerWithTypedFallbacks("illegal");
    // IllegalArgumentException is a RuntimeException, so it also routes to RuntimeException overload:
    assertThat(generalResult).contains("Fallback for RuntimeException");

    // Normal path returns success
    String successResult = resilienceService.circuitBreakerWithTypedFallbacks("none");
    assertThat(successResult).isEqualTo("Success without exception");
  }

  @Test
  @DisplayName("CircuitBreaker state transition: CLOSED -> OPEN -> HALF_OPEN")
  void circuitBreaker_stateTransitions_closedToOpenToHalfOpen() {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("transitionTest", () ->
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(4)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofMillis(200))
            .permittedNumberOfCallsInHalfOpenState(2)
            .build());

    // Initially CLOSED
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

    // Record failures to exceed 50% threshold
    cb.onError(0, TimeUnit.MILLISECONDS, new RuntimeException("fail-1"));
    cb.onError(0, TimeUnit.MILLISECONDS, new RuntimeException("fail-2"));

    // State transitions to OPEN
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    // Wait for open state duration to elapse and transition to HALF_OPEN
    try {
      Thread.sleep(250);
    } catch (InterruptedException ignored) {}

    cb.transitionToHalfOpenState();
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

    // Successful trial call resets to CLOSED
    cb.onSuccess(0, TimeUnit.MILLISECONDS);
    cb.onSuccess(0, TimeUnit.MILLISECONDS);
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  @DisplayName("RateLimiter rejection: Throws RequestNotPermitted when quota is exceeded")
  void rateLimiter_rejectsRequests_whenLimitExceeded() {
    RateLimiter rl = rateLimiterRegistry.rateLimiter("rejectionTest", () ->
        io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofSeconds(10))
            .timeoutDuration(Duration.ZERO)
            .build());

    // First permission granted
    boolean firstAcquire = rl.acquirePermission();
    assertThat(firstAcquire).isTrue();

    // Second permission immediately rejected
    boolean secondAcquire = rl.acquirePermission();
    assertThat(secondAcquire).isFalse();

    assertThatThrownBy(() -> RateLimiter.waitForPermission(rl))
        .isInstanceOf(RequestNotPermitted.class);
  }

  @Test
  @DisplayName("Bulkhead isolation: Throws BulkheadFullException when semaphore limit is saturated")
  void bulkhead_rejectsConcurrentCalls_whenFull() {
    Bulkhead bh = bulkheadRegistry.bulkhead("saturationTest", () ->
        io.github.resilience4j.bulkhead.BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ZERO)
            .build());

    boolean firstAcquire = bh.tryAcquirePermission();
    assertThat(firstAcquire).isTrue();

    // Concurrency limit of 1 reached; next acquisition fails
    boolean secondAcquire = bh.tryAcquirePermission();
    assertThat(secondAcquire).isFalse();

    bh.onComplete(); // release
    assertThat(bh.tryAcquirePermission()).isTrue();
  }

  @Test
  @DisplayName("TimeLimiter bound: Fails exceptionally when async execution exceeds duration threshold")
  void timeLimiter_timesOutSlowFuture() {
    TimeLimiter tl = timeLimiterRegistry.timeLimiter("timeoutTest", () ->
        io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100))
            .cancelRunningFuture(true)
            .build());

    CompletableFuture<String> slowFuture = CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(500);
      } catch (InterruptedException ignored) {}
      return "slow-result";
    });

    assertThatThrownBy(() -> tl.executeCompletionStage(
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(),
        () -> slowFuture).toCompletableFuture().get())
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(TimeoutException.class);
  }

  @Test
  @DisplayName("Retry mechanics: Retries target operation exactly max-attempts times")
  void retry_attemptsInvocation_upToMaxAttempts() {
    Retry retry = retryRegistry.retry("attemptsTest", () ->
        io.github.resilience4j.retry.RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(10))
            .build());

    AtomicInteger callCount = new AtomicInteger(0);

    assertThatThrownBy(() -> Retry.decorateRunnable(retry, () -> {
      callCount.incrementAndGet();
      throw new RuntimeException("Simulated network blip");
    }).run()).isInstanceOf(RuntimeException.class);

    assertThat(callCount.get()).isEqualTo(3);
  }
}
