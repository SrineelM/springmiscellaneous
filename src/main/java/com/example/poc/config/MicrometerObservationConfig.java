package com.example.poc.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Micrometer's {@code @Observed} AOP. With micrometer-tracing-bridge-otel present,
 * observations are bridged to OpenTelemetry spans so tests can assert on them via the OTel
 * exporter.
 */
@Configuration
public class MicrometerObservationConfig {

  /** Wires the ObservedAspect to weave methods annotated with {@code @Observed}. */
  @Bean
  public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
    return new ObservedAspect(observationRegistry);
  }
}
