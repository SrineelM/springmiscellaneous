package com.example.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the Distributed Tracing and Resilience4j Proof of Concept.
 *
 * <p>
 * Educational value:
 * 1. Demonstrates dual-path tracing: programmatic OpenTelemetry SDK alongside
 * declarative Micrometer Observation (@Observed).
 * 2. Implements all 6 Resilience4j fault-tolerance patterns (CircuitBreaker,
 * Retry, RateLimiter, Bulkhead, TimeLimiter, Cache).
 * 3. Configures @EnableCaching to back the Caffeine/Resilience4j caching
 * patterns.
 */
@SpringBootApplication
@EnableCaching
public class DistributedTracingResiliencePocApplication {

  public static void main(String[] args) {
    // Explicitly configure OTel system properties before Spring initialization
    System.setProperty("otel.java.global-autoconfigure.enabled", "true");
    System.setProperty("otel.metrics.exporter", "none");

    SpringApplication.run(DistributedTracingResiliencePocApplication.class, args);
  }
}
