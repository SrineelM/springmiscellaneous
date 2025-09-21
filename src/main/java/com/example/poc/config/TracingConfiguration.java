package com.example.poc.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.Duration;
import java.time.Instant;

/**
 * Comprehensive OpenTelemetry configuration implementing all features discussed in the chat:
 * - Custom resource attributes with business metadata
 * - W3C trace context and baggage propagation for cross-service tracing
 * - OTLP exporter compatible with AWS X-Ray and Jaeger
 * - Custom sampling and batching strategies for production readiness
 * - Business context attributes as discussed in the architecture overview
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
     * Creates OpenTelemetry SDK with comprehensive resource attributes
     * combining standard OpenTelemetry attributes with custom business metadata
     * as discussed in the distributed tracing implementation strategy
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        
        // Create custom resource with business context as discussed in the chat
        Resource resource = Resource.getDefault()
                .merge(Resource.create(
                    Attributes.builder()
                        // Standard OpenTelemetry semantic conventions
                        .put(ResourceAttributes.SERVICE_NAME, applicationName)
                        .put(ResourceAttributes.SERVICE_NAMESPACE, "ecommerce")
                        .put(ResourceAttributes.SERVICE_VERSION, "1.0.0")
                        .put(ResourceAttributes.SERVICE_INSTANCE_ID, 
                             System.getProperty("instance.id", java.net.InetAddress.getLoopbackAddress().getHostName()))
                        .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, 
                             System.getProperty("environment", "development"))
                        
                        // Custom business attributes as discussed in the architecture overview
                        .put("app.name", "DistributedTracingPOC")
                        .put("product.code", productCode)
                        .put("business.unit", businessUnit)
                        .put("cost.center", costCenter)
                        .put("data.classification", dataClassification)
                        
                        // Build and deployment metadata for production traceability
                        .put("build.version", getClass().getPackage().getImplementationVersion() != null ? 
                             getClass().getPackage().getImplementationVersion() : "dev-build")
                        .put("build.timestamp", Instant.now().toString())
                        .put("deployment.type", "on-premises-monolith") // As per chat architecture
                        
                        // Infrastructure metadata
                        .put("infrastructure.layer", "layer-1-monolith")
                        .put("java.version", System.getProperty("java.version"))
                        .put("spring.boot.version", org.springframework.boot.SpringBootVersion.getVersion())
                        .build()
                ));
        
        return OpenTelemetrySdk.builder()
                .setTracerProvider(
                    SdkTracerProvider.builder()
                        .setResource(resource)
                        .addSpanProcessor(BatchSpanProcessor.builder(
                            OtlpGrpcSpanExporter.builder()
                                .setEndpoint(otlpEndpoint) // Compatible with AWS X-Ray via ADOT Collector
                                .setCompression("gzip") // Optimize network traffic
                                .build())
                            .setMaxExportBatchSize(512) // Batch size for efficiency
                            .setScheduleDelay(Duration.ofMillis(500)) // Regular export interval
                            .setMaxQueueSize(2048) // Queue size for high throughput
                            .build())
                        .setSampler(Sampler.alwaysOn()) // For POC - use probabilistic sampling in production
                        .build())
                // W3C standard propagators for cross-service tracing as discussed
                .setPropagators(ContextPropagators.create(
                    TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(), // Standard trace context
                        W3CBaggagePropagator.getInstance() // Business context propagation
                    )))
                .buildAndRegisterGlobal();
    }
}