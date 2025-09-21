package com.example.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * =================================================================================================
 * ARCHITECTURAL REVIEW
 * =================================================================================================
 *
 * <p>This is the main entry point for the Spring Boot application.
 *
 * <p>Key Architectural Decisions & Best Practices: ------------------------------------------------
 * 1. `@SpringBootApplication`: Standard annotation that enables auto-configuration, component
 * scanning, and property support. This is the foundation of the application. 2. `@EnableCaching`:
 * This annotation is crucial for activating Spring's caching abstraction. In this POC, it enables
 * the `@Cacheable` annotation used in `ExternalServiceClient` for the Resilience4j Cache pattern
 * demonstration. It's a good practice to enable features explicitly. 3. System Properties for
 * OpenTelemetry: - `otel.java.global-autoconfigure.enabled`: Setting this to `true` leverages the
 * OpenTelemetry Java agent's auto-configuration capabilities, which simplifies setup. -
 * `otel.metrics.exporter`: Explicitly disabling the metrics exporter (`none`) is a smart choice for
 * this POC, as it focuses purely on distributed tracing. This reduces overhead and noise, allowing
 * developers to concentrate on the tracing aspect. In a real production scenario, this would likely
 * be configured to export to a monitoring system like Prometheus.
 *
 * <p>Overall Feedback: ----------------- - The class is well-structured and serves its purpose as
 * the application's bootstrap point. - The comments provide excellent context about the POC's goals
 * and architecture, which is invaluable for new developers. - The explicit enabling of caching and
 * configuration of OpenTelemetry system properties directly in the main method are clear and
 * effective for a self-contained POC. For a larger application, these properties might be moved to
 * a configuration file or startup script.
 *
 * <p>This class is a solid starting point for the application, correctly setting up the necessary
 * features for the demonstration.
 * =================================================================================================
 */
@SpringBootApplication
@EnableCaching
public class DistributedTracingResiliencePocApplication {

  public static void main(String[] args) {
    // Set system properties for better OpenTelemetry performance
    System.setProperty("otel.java.global-autoconfigure.enabled", "true");
    System.setProperty("otel.metrics.exporter", "none"); // Focus on traces for this POC

    SpringApplication.run(DistributedTracingResiliencePocApplication.class, args);
  }
}
