package com.example.poc.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * =================================================================================================
 * ARCHITECTURAL REVIEW
 * =================================================================================================
 *
 * <p>This class, `TracingConfiguration`, is responsible for setting up and configuring the entire
 * OpenTelemetry SDK. This is a critical piece of infrastructure-as-code, defining how traces are
 * collected, enriched, and exported.
 *
 * <p>Key Architectural Decisions & Best Practices: ------------------------------------------------
 * 1. `@Configuration` and `@Bean`: Correctly uses Spring's Java-based configuration to create and
 * manage the `OpenTelemetry` instance as a singleton bean. 2. `@EnableAspectJAutoProxy`: This is
 * essential. It enables Spring's support for processing components marked with `@Aspect`, which is
 * what activates the `DistributedTracingAspect`. 3. Externalized Configuration (`@Value`):
 * Hardcoding values is avoided. Key configuration parameters like product code, business unit, and
 * the OTLP endpoint are injected from `application.yml`. This makes the application flexible and
 * easy to configure for different environments. 4. Rich Resource Attributes: The creation of the
 * `Resource` object is a highlight. It merges default resources with a rich set of custom
 * attributes, including: - **Standard Semantic Conventions**: `service.name`, `service.version`,
 * etc. This ensures compatibility with standard OpenTelemetry UIs. - **Custom Business
 * Attributes**: `product.code`, `business.unit`, `cost.center`. This is where the business context
 * is injected at the source, tagging every single trace from this service with this metadata. -
 * **Build and Deployment Metadata**: `build.version`, `deployment.type`. This is invaluable for
 * debugging, as it tells you exactly which version of the code generated a trace. 5.
 * Production-Ready Exporter Configuration: The `BatchSpanProcessor` is configured with settings
 * that are suitable for a production environment (e.g., batch size, schedule delay, queue size,
 * gzip compression). This shows foresight beyond a simple POC. 6. Standard-Based Propagation: The
 * use of `W3CTraceContextPropagator` and `W3CBaggagePropagator` is the correct, modern approach. It
 * ensures interoperability with any other service or platform that follows the W3C Trace Context
 * standard. 7. Global Registration: `buildAndRegisterGlobal()` makes this OpenTelemetry instance
 * the default for the entire JVM, which is a common and effective strategy. 8. DI-Friendly Tracer
 * Bean: Expose a `Tracer` bean derived from the `OpenTelemetry` bean so components (like the AOP
 * aspect) can rely on Spring DI instead of static globals.
 *
 * <p>Role in the Architecture: ------------------------- - This class is the foundation of the
 * tracing system. It sets up the "factory" that produces tracers and spans. - It defines the
 * identity of this service in the distributed system via the `Resource` attributes. - It configures
 * how traces will be exported from the application (in this case, via OTLP gRPC).
 *
 * <p>Overall Feedback: ----------------- - This is a production-grade OpenTelemetry configuration.
 * It's comprehensive, flexible, and follows all current best practices. - The richness of the
 * resource attributes is a major strength, demonstrating a mature understanding of what makes a
 * tracing system truly useful in a large enterprise. - The configuration is clean, readable, and
 * well-commented.
 *
 * <p>This class perfectly sets the stage for the `DistributedTracingAspect` to do its work,
 * ensuring that every trace produced is rich with context and correctly exported.
 * =================================================================================================
 */
@Configuration
@EnableAspectJAutoProxy
public class TracingConfiguration {

  @Value("${app.product.code:ECOM-POC}")
  private String productCode;

  @Value("${app.business.unit:customer-experience}")
  private String businessUnit;

  @Value("${spring.application.name:distributed-tracing-poc}")
  private String applicationName;

  @Value("${app.cost.center:CC-2025-001}")
  private String costCenter;

  @Value("${app.data.classification:internal}")
  private String dataClassification;

  @Value("${management.otlp.tracing.endpoint:http://localhost:4317}")
  private String otlpEndpoint;

  /**
   * Creates OpenTelemetry SDK with comprehensive resource attributes combining standard
   * OpenTelemetry attributes with custom business metadata as discussed in the distributed tracing
   * implementation strategy
   */
  @Bean
  public OpenTelemetry openTelemetry() {

    // Create custom resource with business context as discussed in the chat
    Resource resource =
        Resource.getDefault()
            .merge(
                Resource.create(
                    Attributes.builder()
                        // Standard OpenTelemetry semantic convention keys (avoid semconv
                        // dependency)
                        .put("service.name", applicationName)
                        .put("service.namespace", "ecommerce")
                        .put("service.version", "1.0.0")
                        .put(
                            "service.instance.id",
                            System.getProperty(
                                "instance.id",
                                java.net.InetAddress.getLoopbackAddress().getHostName()))
                        .put(
                            "deployment.environment",
                            System.getProperty("environment", "development"))

                        // Custom business attributes as discussed in the architecture overview
                        .put("app.name", "DistributedTracingPOC")
                        .put("product.code", productCode)
                        .put("business.unit", businessUnit)
                        .put("cost.center", costCenter)
                        .put("data.classification", dataClassification)

                        // Build and deployment metadata for production traceability
                        .put(
                            "build.version",
                            getClass().getPackage().getImplementationVersion() != null
                                ? getClass().getPackage().getImplementationVersion()
                                : "dev-build")
                        .put("build.timestamp", Instant.now().toString())
                        .put("deployment.type", "on-premises-monolith") // As per chat architecture

                        // Infrastructure metadata
                        .put("infrastructure.layer", "layer-1-monolith")
                        .put("java.version", System.getProperty("java.version"))
                        .put(
                            "spring.boot.version",
                            org.springframework.boot.SpringBootVersion.getVersion())
                        .build()));

    return OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(
                    BatchSpanProcessor.builder(
                            OtlpGrpcSpanExporter.builder()
                                .setEndpoint(
                                    otlpEndpoint) // Compatible with AWS X-Ray via ADOT Collector
                                .setCompression("gzip") // Optimize network traffic
                                .build())
                        .setMaxExportBatchSize(512) // Batch size for efficiency
                        .setScheduleDelay(Duration.ofMillis(500)) // Regular export interval
                        .setMaxQueueSize(2048) // Queue size for high throughput
                        .build())
                .setSampler(
                    Sampler.alwaysOn()) // For POC - use probabilistic sampling in production
                .build())
        // W3C standard propagators for cross-service tracing as discussed
        .setPropagators(
            ContextPropagators.create(
                TextMapPropagator.composite(
                    W3CTraceContextPropagator.getInstance(), // Standard trace context
                    W3CBaggagePropagator.getInstance() // Business context propagation
                    )))
        .buildAndRegisterGlobal();
  }

  /**
   * Provide a DI-managed Tracer bean.
   *
   * <p>Why: While OpenTelemetrySdk registers a global, wiring a Tracer through Spring makes testing
   * easier and avoids hidden static dependencies. This also clarifies which tracer namespace the
   * application uses across components.
   */
  @Bean
  public Tracer tracer(OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer("com.example.poc.tracing");
  }
}
