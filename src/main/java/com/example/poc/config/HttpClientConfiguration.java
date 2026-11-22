package com.example.poc.config;

import com.example.poc.tracing.TracingRestTemplateInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for HTTP clients with automatic distributed tracing.
 *
 * <p>This configuration demonstrates: - RestTemplate with automatic OpenTelemetry tracing - HTTP
 * request/response tracing with W3C context propagation - CLIENT span creation for outbound calls
 */
@Configuration
public class HttpClientConfiguration {

  /**
   * Creates a RestTemplate with tracing interceptor for automatic context propagation.
   *
   * <p>This bean demonstrates the PROGRAMMATIC approach to distributed tracing for HTTP clients.
   * The interceptor automatically: - Creates CLIENT spans for each request - Propagates W3C trace
   * context headers - Adds HTTP semantic attributes - Records exceptions and response status
   *
   * @param tracer OpenTelemetry tracer
   * @param openTelemetry OpenTelemetry instance for context propagation
   * @return Configured RestTemplate with tracing
   */
  @Bean
  public RestTemplate tracedRestTemplate(Tracer tracer, OpenTelemetry openTelemetry) {
    TracingRestTemplateInterceptor interceptor =
        new TracingRestTemplateInterceptor(tracer, openTelemetry);

    return new RestTemplateBuilder().additionalInterceptors(interceptor).build();
  }

  /**
   * Optional: Create a non-traced RestTemplate for scenarios where tracing is not desired.
   *
   * <p>Use this when you want to make HTTP calls without creating spans (e.g., health checks,
   * internal communication that shouldn't be traced).
   */
  @Bean
  public RestTemplate plainRestTemplate() {
    return new RestTemplateBuilder().build();
  }
}
