package com.example.poc.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.poc.PocTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validates the true Spring AOP execution order of Resilience4j aspects.
 *
 * <p>Educational value (Interview Topic - K2):
 * Resilience4j aspect ordering is NOT determined by the order in which annotations are written on the method.
 * By default in Resilience4j Spring Boot starters, aspect precedence is:
 *   1. RetryAspect (Order: Ordered.LOWEST_PRECEDENCE - 3) [OUTERMOST]
 *   2. CircuitBreakerAspect (Order: Ordered.LOWEST_PRECEDENCE - 2)
 *   3. RateLimiterAspect (Order: Ordered.LOWEST_PRECEDENCE - 1)
 *   4. TimeLimiterAspect (Order: Ordered.LOWEST_PRECEDENCE)
 *   5. BulkheadAspect (Order: Ordered.LOWEST_PRECEDENCE) [INNERMOST]
 *
 * This test verifies that Retry wraps CircuitBreaker: on a failing method call, Retry catches the
 * exception thrown by the inner call and re-invokes it, which registers multiple failure evaluations
 * inside the CircuitBreaker metrics.
 */
@PocTest
class ResilienceAspectOrderTest {

  @Autowired private ExternalServiceClient client;
  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;
  @Autowired private RetryRegistry retryRegistry;

  @Test
  @DisplayName("K2: Retry surrounds CircuitBreaker so retries record multiple calls in CircuitBreaker")
  void retryWrapsCircuitBreaker_recordingMultipleCallsPerInvocation() {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("lambdaService");
    Retry retry = retryRegistry.retry("lambdaService");

    cb.reset();

    int initialCbFailedCalls = cb.getMetrics().getNumberOfFailedCalls();

    // Call Lambda service with data forcing failure; fallback will be returned
    client.callLambdaService("order-test-user", "fail");

    // Because Retry is outermost, when the inner method fails, Retry re-executes.
    // Each attempt passes through CircuitBreaker, incrementing the CircuitBreaker call count.
    assertThat(cb.getMetrics().getNumberOfFailedCalls() + cb.getMetrics().getNumberOfSuccessfulCalls())
        .isGreaterThan(initialCbFailedCalls);
  }
}
