# GitHub Copilot Instructions for Distributed Tracing & Resilience POC

This file provides context and guidelines for GitHub Copilot when working with this Spring Boot distributed tracing and resilience patterns project.

## Project Overview

This is a **Spring Boot 3.5.x** application demonstrating:
- **Dual Tracing Frameworks**: OpenTelemetry (programmatic) + Micrometer (declarative)
- **Complete Resilience4j**: All 6 patterns with multiple configurations
- **AspectJ AOP**: Custom business annotations for automatic tracing
- **Business Context Propagation**: Hierarchical IDs and W3C baggage

## Key Technologies

### Core Framework
- **Spring Boot 3.5.x** with Java 17+
- **Gradle 8.5+** build system
- **AspectJ** for AOP-based tracing

### Tracing Stack
- **OpenTelemetry SDK 1.40.0**: Custom configuration with OTLP export
- **Micrometer Tracing**: With `micrometer-tracing-bridge-otel`
- **W3C Trace Context**: Standard propagation format
- **Baggage API**: Cross-service context sharing

### Resilience Stack
- **Resilience4j 2.3.0**: All patterns (CircuitBreaker, Retry, RateLimiter, Bulkhead, TimeLimiter, Cache)
- **Caffeine Cache**: High-performance caching backend
- **Spring AOP**: Annotation-driven resilience

## Code Style Guidelines

### General Principles
1. **Non-Intrusive**: Business logic should remain clean; use AOP for cross-cutting concerns
2. **Declarative First**: Prefer annotations over programmatic code where possible
3. **Explicit Naming**: Use descriptive names for spans, operations, and business contexts
4. **Low Cardinality**: Keep tag/attribute values bounded to avoid metric explosion

### Tracing Patterns

#### When to Use OpenTelemetry (Programmatic)
```java
// Use for: Fine-grained control, custom spans, async workflows
TracingUtils.withSpan("custom-operation", span -> {
    span.setAttribute("business.context.key", "value");
    return performOperation();
});
```

#### When to Use Micrometer (Declarative)
```java
// Use for: Standard method instrumentation, metrics + tracing
@Observed(name = "customer.service.find", contextualName = "find-customer")
public Customer findById(Long customerId) {
    return repository.findById(customerId);
}
```

#### Hybrid Approach (Both Together)
```java
// High-level: Micrometer observation
@Observed(name = "order.processing")
public OrderResult processOrder(Order order) {
    // Low-level: OpenTelemetry for specific sub-operations
    TracingUtils.withSpan("validate-inventory", span -> {
        span.setAttribute("sku", order.getSku());
        return inventoryService.validate(order);
    });
}
```

### Resilience4j Patterns

#### Pattern Ordering (Important!)
When combining multiple patterns, order matters:
```java
@RateLimiter(name = "api")           // 1. Control rate first
@CircuitBreaker(name = "api")         // 2. Fast-fail if service down
@Retry(name = "api")                  // 3. Retry transient failures
@Bulkhead(name = "api")               // 4. Limit concurrency
@TimeLimiter(name = "api")            // 5. Timeout protection
public Response callExternalApi() { }
```

#### Fallback Methods
```java
// Fallback signature must match original method + Exception parameter
@CircuitBreaker(name = "service", fallbackMethod = "fallbackResponse")
public ProcessingResult callService(String userId, String data) { }

// Fallback method
public ProcessingResult fallbackResponse(String userId, String data, Exception ex) {
    logger.warn("Fallback triggered: {}", ex.getMessage());
    return ProcessingResult.builder()
        .status("FALLBACK")
        .message("Using cached/default response")
        .build();
}
```

### Custom Annotations

#### @BusinessOperation
```java
// Use for: High-level business operations with semantic meaning
@BusinessOperation(
    name = "payment-processing",
    category = "financial",
    criticality = "critical",
    sensitive = true,
    expectedDuration = "medium"
)
```

#### @TraceMethod
```java
// Use for: Fine-grained method tracing with argument capture
@TraceMethod(
    operationName = "database-query",
    includeArgs = true,         // Capture method arguments
    includeReturnValue = false, // Don't capture return (large data)
    skipTracing = false
)
```

#### @Observed
```java
// Use for: Micrometer observations (metrics + tracing)
@Observed(
    name = "repository.operation",           // Low-cardinality name
    contextualName = "find-by-id",           // Operation variant
    lowCardinalityKeyValues = {"layer", "repository"}  // Tags
)
```

## Naming Conventions

### Span Names
- **Controllers**: Use HTTP method + endpoint pattern (auto-generated)
- **Services**: Use `service-name.operation` (e.g., `customer.service.find`)
- **Repositories**: Use `repository-name.operation` (e.g., `customer.repository.findById`)
- **External Calls**: Use `client.service-name` (e.g., `client.lambda-service`)

### Business IDs
```
Transaction ID: ECOM-POC-DEV-20250920135400-A1B2-001234-A7F3
Correlation ID: COR-ECOM-POC-A1B2-12AB34CD-E5F6789A
Session ID:     SES-ECOM-POC-A1B2-A1B2C3D4E5F67890-12345678
```

### Baggage Keys
Use **dotted notation** for consistency:
```java
baggage.set("user.id", userId);
baggage.set("business.transaction.id", transactionId);
baggage.set("action.type", actionType);
```

## Configuration Best Practices

### Environment-Specific Settings

#### Development
```yaml
resilience4j:
  circuitbreaker:
    instances:
      service:
        failure-rate-threshold: 30  # More lenient
management:
  tracing:
    sampling:
      probability: 1.0              # 100% sampling
```

#### Production
```yaml
resilience4j:
  circuitbreaker:
    instances:
      service:
        failure-rate-threshold: 60  # Stricter
management:
  tracing:
    sampling:
      probability: 0.1              # 10% sampling
```

### Secret Management
```yaml
# NEVER hardcode secrets in application.yml!
app:
  security:
    # Use environment variables or secret managers
    salt: ${SECURITY_SALT:default-dev-salt}
```

## Testing Guidelines

### Unit Tests
```java
// Mock the ObservationRegistry for Micrometer
@MockBean
private ObservationRegistry observationRegistry;

// Use TestObservationRegistry for integration
@TestConfiguration
static class TestConfig {
    @Bean
    public ObservationRegistry observationRegistry() {
        return TestObservationRegistry.create();
    }
}
```

### Integration Tests
```java
@SpringBootTest
@AutoConfigureObservability  // Auto-configure tracing
class TracingIntegrationTest {

    @Autowired
    private TestObservationRegistry registry;

    @Test
    void shouldCreateSpans() {
        // Execute operation
        service.performOperation();

        // Verify spans created
        assertThat(registry.getFinishedSpans())
            .hasSize(3)
            .extracting("name")
            .contains("service.operation");
    }
}
```

## Common Patterns

### Async Tracing
```java
@Async
public CompletableFuture<Result> asyncOperation() {
    // Context automatically propagated with TracingUtils
    return CompletableFuture.supplyAsync(
        TracingUtils.wrapWithCurrentContext(() -> {
            return performWork();
        })
    );
}
```

### HTTP Client Tracing
```java
// RestTemplate automatically instrumented via TracingRestTemplateInterceptor
@Autowired
private RestTemplate restTemplate;

public String callExternalService() {
    // CLIENT span automatically created with W3C headers
    return restTemplate.getForObject(url, String.class);
}
```

### Baggage Propagation
```java
// Set baggage in service A
TracingUtils.setBaggage("tenant.id", tenantId);

// Automatically available in service B
String tenantId = TracingUtils.getBaggage("tenant.id");
```

## Performance Considerations

### Span Volume Control
```java
// For high-traffic endpoints, consider:

// 1. Skip tracing for health checks
@TraceMethod(skipTracing = true)
public HealthStatus checkHealth() { }

// 2. Use span events instead of nested spans
Span currentSpan = Span.current();
currentSpan.addEvent("cache-lookup-started");
// ... perform operation ...
currentSpan.addEvent("cache-lookup-completed");

// 3. Sample strategically in production (10-20%)
management.tracing.sampling.probability: 0.1
```

### Cardinality Management
```java
// ❌ BAD: High cardinality (unique per request)
span.setAttribute("user.email", "unique@email.com");
span.setAttribute("request.id", UUID.randomUUID().toString());

// ✅ GOOD: Low cardinality (bounded values)
span.setAttribute("user.role", "ADMIN");  // Limited set
span.setAttribute("request.status", "SUCCESS");  // Fixed values
span.setAttribute("region", "us-east-1");  // Known regions
```

## Security Guidelines

### Sensitive Data Handling
```java
// Mark operations handling sensitive data
@BusinessOperation(
    name = "payment-processing",
    sensitive = true  // Prevents detailed logging
)

// Don't include sensitive data in spans
@TraceMethod(
    includeArgs = false,  // Skip PII in arguments
    includeReturnValue = false
)
public PaymentResult processPayment(CreditCard card) { }

// Hash user identifiers
String hashedUserId = hashWithSalt(userId);
span.setAttribute("user.id.hash", hashedUserId);
```

### Actuator Security
```yaml
# Protect actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info  # Only these public in prod
```

## Migration Path

### From OpenTelemetry to Micrometer
```java
// Before: OpenTelemetry programmatic
public void oldStyle() {
    Span span = tracer.spanBuilder("operation").startSpan();
    try (Scope scope = span.makeCurrent()) {
        span.setAttribute("key", "value");
        // ... operation ...
    } finally {
        span.end();
    }
}

// After: Micrometer declarative
@Observed(name = "operation")
public void newStyle() {
    // ... operation ...
    // Spans created automatically
}
```

### Gradual Adoption
```java
// Keep both during migration
@Observed(name = "customer.service.update")  // New
@TraceMethod(operationName = "update-customer")  // Old (can remove later)
public Customer updateCustomer(Customer customer) {
    // Both create spans; choose one for production
}
```

## Troubleshooting

### Common Issues

#### Issue: Spans not appearing
```java
// ✅ Ensure ObservationRegistry is autowired
@Autowired
private ObservationRegistry registry;

// ✅ Check sampling configuration
management.tracing.sampling.probability: 1.0  // 100% for dev
```

#### Issue: Context not propagating to async
```java
// ❌ Wrong: Context lost
CompletableFuture.supplyAsync(() -> operation());

// ✅ Correct: Wrap with current context
CompletableFuture.supplyAsync(
    TracingUtils.wrapWithCurrentContext(() -> operation())
);
```

#### Issue: Baggage not propagating
```yaml
# ✅ Ensure baggage keys configured
management:
  tracing:
    baggage:
      correlation:
        fields: user.id,tenant.id
      remote-fields: user.id,tenant.id
```

## Additional Resources

- **OpenTelemetry**: https://opentelemetry.io/docs/java/
- **Micrometer**: https://micrometer.io/docs/tracing
- **Resilience4j**: https://resilience4j.readme.io/
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/reference/actuator/

## Project-Specific Notes

### Demo Endpoints

#### Tracing Demos
- OpenTelemetry: `/api/v1/tracing/otel/*`
- Micrometer: `/api/v1/tracing/micrometer/*`
- Hybrid: `/api/v1/tracing/hybrid`
- Comparison: `/api/v1/tracing/comparison`

#### Resilience4j Demos
- Circuit Breaker: `/api/v1/resilience/circuit-breaker/*`
- Retry: `/api/v1/resilience/retry/*`
- Rate Limiter: `/api/v1/resilience/rate-limiter/*`
- Bulkhead: `/api/v1/resilience/bulkhead/*`
- Time Limiter: `/api/v1/resilience/time-limiter`
- Cache: `/api/v1/resilience/cache/*`
- Combined: `/api/v1/resilience/combined/*`

#### Production Flow
- Main endpoint: `/api/v1/processing/complete-flow`
- Demonstrates all patterns together with real business context

---

**When generating code for this project, Copilot should:**
1. ✅ Follow the dual tracing approach (OpenTelemetry + Micrometer)
2. ✅ Use appropriate Resilience4j patterns for fault tolerance
3. ✅ Include proper business context and correlation IDs
4. ✅ Follow low-cardinality principles for attributes
5. ✅ Add comprehensive JavaDoc and inline comments
6. ✅ Include unit and integration tests
7. ✅ Secure sensitive data and use fallback methods
8. ✅ Provide endpoint examples in JavaDoc
