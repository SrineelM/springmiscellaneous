package com.example.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main application class for Distributed Tracing with Resilience4j POC
 * 
 * This POC demonstrates:
 * 1. Complete distributed tracing with OpenTelemetry across multi-tier architecture
 * 2. Custom business context injection and ID generation following enterprise patterns
 * 3. AspectJ-based automatic instrumentation with business annotations
 * 4. All Resilience4j patterns: Circuit Breaker, Retry, Rate Limiter, Bulkhead, Time Limiter, Cache
 * 5. Baggage propagation for cross-service context sharing
 * 6. Custom resource attributes for business metadata
 * 7. Comprehensive error handling and fallback mechanisms
 * 8. Production-ready configuration and monitoring endpoints
 * 
 * Architecture simulates:
 * - On-premises Java Spring monolith (this POC)
 * - AWS Lambda Python microservice (simulated)
 * - EKS Java Spring microservice (simulated)
 * 
 * Features from the chat implemented:
 * - Business transaction ID generation with custom format
 * - Correlation ID propagation across service boundaries
 * - User context capture and baggage propagation
 * - Custom resource attributes for product code, business unit
 * - Structured logging with trace context integration
 * - X-Ray compatible trace export (via OTLP)
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