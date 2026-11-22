# Distributed Tracing and Resilience4j Examples POC

A comprehensive Proof of Concept demonstrating **enterprise-grade distributed tracing** across a multi-tier architecture with **complete fault tolerance** using OpenTelemetry and all Resilience4j patterns.

## 🏗️ Architecture Overview

This POC simulates a real-world distributed architecture commonly found in enterprise environments:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Layer 1       │    │     Layer 2      │    │    Layer 3      │
│ On-premises     │───▶│ AWS Lambda       │───▶│ EKS Java        │
│ Java Spring     │    │ Python           │    │ Spring          │
│ Monolith        │    │ Microservice     │    │ Microservice    │
│ (This POC)      │    │ (Simulated)      │    │ (Simulated)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 ▼
                    ┌─────────────────────────┐
                    │   Data Layer            │
                    │   Database Operations   │
                    │   (Simulated)           │
                    └─────────────────────────┘
```

### What This POC Demonstrates

- **🔍 End-to-End Distributed Tracing**: Complete visibility across all service layers
- **🛡️ Fault Tolerance**: All 6 Resilience4j patterns working together
- **📊 Business Context Propagation**: Custom business IDs and metadata
- **⚡ Automatic Instrumentation**: AspectJ-based tracing with zero code intrusion
- **📈 Production Monitoring**: Comprehensive metrics and health checks
- **🚀 Enterprise Ready**: Production-grade configuration and error handling

## ✨ Features Implemented

### 🚀 NEW: Production-Ready Enhancements
- ✅ **Scalable ID Generation**: Business and correlation IDs now include a unique `instanceId` to prevent collisions in multi-instance, horizontally-scaled environments (e.g., Kubernetes).
- ✅ **Enhanced Security**: User identifiers in session IDs are now protected using a salted **SHA-256 hash**, preventing reverse-engineering and protecting user privacy in logs and traces.
- ✅ **Production Configuration Best Practices**: The `application.yml` now includes detailed comments explaining how to manage secrets (like the hashing salt) and configurations in a real production environment using tools like Spring Cloud Config and HashiCorp Vault.

### 🔍 OpenTelemetry Distributed Tracing

#### Core Tracing Features
- ✅ **Custom Resource Attributes**: Business metadata (product code, business unit, cost center)
- ✅ **Business Context Propagation**: Custom ID generation and baggage propagation
- ✅ **Correlation ID Management**: Automatic generation and header-based extraction
- ✅ **User Context Tracking**: User ID, session ID, and action type propagation
- ✅ **Structured Logging**: MDC integration with trace context
- ✅ **AspectJ Instrumentation**: Automatic tracing with business annotations
- ✅ **OTLP Export**: Compatible with AWS X-Ray via ADOT Collector

#### Business Context Features
- **Custom Business ID Generation**: Format `ECOM-POC-DEV-20250920135400-001234-A7F3`
- **Hierarchical Correlation**: Transaction → Correlation → Session IDs
- **Baggage Propagation**: Cross-service context sharing
- **HTTP Header Extraction**: Automatic user context from headers
- **Sensitive Data Masking**: Secure logging for compliance

### 🛡️ Resilience4j Patterns (Complete Implementation)

#### 1. Circuit Breaker Pattern
```java
@CircuitBreaker(name = "lambdaService", fallbackMethod = "fallbackLambdaCall")
```
- **Purpose**: Prevents cascading failures across services
- **Configuration**: 50% failure rate threshold, 10-call sliding window
- **Fallback**: Graceful degradation with cached responses

#### 2. Retry Pattern
```java
@Retry(name = "lambdaService")
```
- **Purpose**: Automatic retry with exponential backoff
- **Configuration**: 3 attempts, 1s initial delay, 2x multiplier
- **Smart Retry**: Only on transient failures

#### 3. Rate Limiter Pattern
```java
@RateLimiter(name = "lambdaService")
```
- **Purpose**: Prevents service overload and ensures fair usage
- **Configuration**: 10 requests per second per service
- **Behavior**: Fast-fail when limits exceeded

#### 4. Bulkhead Pattern
```java
@Bulkhead(name = "eksService", type = Bulkhead.Type.SEMAPHORE)
```
- **Purpose**: Resource isolation prevents resource starvation
- **Types**: Semaphore (3 concurrent) and Thread Pool (4 threads)
- **Protection**: One slow service can't consume all resources

#### 5. Time Limiter Pattern
```java
@TimeLimiter(name = "databaseService")
```
- **Purpose**: Prevents operations from hanging indefinitely
- **Configuration**: 3-second timeout with future cancellation
- **Usage**: Async operations with CompletableFuture

#### 6. Cache Pattern
```java
@Cacheable(value = "userDataCache")
```
- **Purpose**: Improves performance and reduces external calls
- **Configuration**: 5-minute TTL, 1000 max entries
- **Intelligence**: Automatic cache key generation

### 🎯 Custom Business Annotations

#### @BusinessOperation
```java
@BusinessOperation(
    name = "complete-processing-flow",
    category = "request-processing",
    criticality = "high",
    expectedDuration = "slow"
)
```

#### @TraceMethod
```java
@TraceMethod(
    operationName = "lambda-service-call",
    includeArgs = true,
    includeReturnValue = true
)
```

#### @ActionType
```java
@ActionType("COMPLETE_PROCESSING")
```

## 🚀 Quick Start Guide

### Prerequisites
- **Java 17+** (developed with Java 23)
- **Gradle 8.5+** (wrapper included)
- **curl** for testing (or any HTTP client)

### 1. Clone and Setup
```bash
git clone <repository-url>
cd springmiscellaneous
```

### 2. Build the Project
```bash
./gradlew compileJava
```

### 3. Run the Application
```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### 4. Run the Integration Tests
To verify that all changes, including the new scalable and secure ID generation, are working correctly, run the integration tests:
```bash
./gradlew test
```
This command will execute all tests in `DistributedTracingIntegrationTest.java`, including the newly added test case for validating the ID formats.

### 5. Verify Startup
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## 🧪 Testing the POC

### Test 1: Complete Distributed Processing Flow (with new secure & scalable IDs)

This endpoint now demonstrates the **full distributed tracing flow** with the newly enhanced, production-ready ID generation:

```bash
curl -X POST http://localhost:8080/api/v1/processing/complete-flow \
  -H "Content-Type: application/json" \
  -H "User-ID: john-doe-123" \
  -H "Session-ID: session-789" \
  -H "X-Correlation-ID: test-correlation-001" \
  -d '{
    "operationId": "test-operation-001",
    "data": "sample-processing-data",
    "parameters": "env=prod,region=us-east-1",
    "priority": "HIGH",
    "category": "BUSINESS_CRITICAL"
  }'
```

**What This Tests:**
- ✅ AspectJ automatic tracing
- ✅ Business context extraction from headers
- ✅ Custom business ID generation
- ✅ Circuit Breaker (Lambda service simulation)
- ✅ Bulkhead (EKS service simulation)
- ✅ Time Limiter (Database operations)
- ✅ Retry mechanisms
- ✅ Rate limiting
- ✅ Complex business logic resilience
- ✅ Baggage propagation across all layers
- ✅ **NEW**: Generation of scalable `instanceId` in transaction and correlation IDs.
- ✅ **NEW**: Secure, salted SHA-256 hashing of the `User-ID` for the session ID.

### Test 2: Cache Pattern Demonstration

```bash
# First call - hits the service (slower)
curl http://localhost:8080/api/v1/processing/user-data/user123

# Second call - uses cache (faster)
curl http://localhost:8080/api/v1/processing/user-data/user123
```

**What This Tests:**
- ✅ Cache hit/miss behavior
- ✅ Performance improvement
- ✅ Rate limiting on cached endpoints

### Test 3: Heavy Processing (Thread Pool Bulkhead)

```bash
curl -X POST "http://localhost:8080/api/v1/processing/heavy-processing?dataId=heavy-test-001&dataSizeMb=50" \
  -H "User-ID: heavy-processing-user"
```

**What This Tests:**
- ✅ Thread Pool Bulkhead isolation
- ✅ Resource protection under load
- ✅ Parameter-based business context

### Test 4: Individual Resilience Patterns

Test each pattern individually:

```bash
# Circuit Breaker
curl "http://localhost:8080/api/v1/processing/test/circuit-breaker?userId=cb-test&data=test"

# Retry Pattern
curl "http://localhost:8080/api/v1/processing/test/retry?userId=retry-test&data=test"

# Rate Limiter
curl "http://localhost:8080/api/v1/processing/test/rate-limiter?userId=rl-test"

# Bulkhead
curl "http://localhost:8080/api/v1/processing/test/bulkhead?userId=bulkhead-test"

# Time Limiter
curl "http://localhost:8080/api/v1/processing/test/time-limiter?userId=tl-test"

# Cache
curl "http://localhost:8080/api/v1/processing/test/cache?userId=cache-test"

# Combined Patterns
curl "http://localhost:8080/api/v1/processing/test/combined?userId=combined-test&data=complex"
```

### Test 5: System Health and Monitoring

```bash
# Application Health
curl http://localhost:8080/api/v1/processing/health

# System Information
curl http://localhost:8080/api/v1/processing/info
```

## 📊 Monitoring and Observability

### Actuator Endpoints

Monitor all Resilience4j patterns in real-time:

```bash
# Circuit Breaker Status
curl http://localhost:8080/actuator/circuitbreakers

# Rate Limiter Metrics
curl http://localhost:8080/actuator/ratelimiters

# Retry Statistics
curl http://localhost:8080/actuator/retries

# Bulkhead Metrics
curl http://localhost:8080/actuator/bulkheads

# Time Limiter Metrics
curl http://localhost:8080/actuator/timelimiters

# All Application Metrics
curl http://localhost:8080/actuator/metrics
```

Tip: In production, authenticate to non-public endpoints:

```bash
curl -u "$ACTUATOR_USER:$ACTUATOR_PASSWORD" http://localhost:8080/actuator/metrics
```

### Sample Circuit Breaker Response
```json
{
  "circuitBreakers": {
    "lambdaService": {
      "failureRate": "25.0%",
      "slowCallRate": "10.0%",
      "failureRateThreshold": "50.0%",
      "state": "CLOSED",
      "bufferedCalls": 8,
      "failedCalls": 2,
      "slowCalls": 1,
      "notPermittedCalls": 0
    }
  }
}
```

## 🎯 Business Context Features

### Automatic Business ID Generation (Now Scalable & Secure)

The POC generates hierarchical, scalable, and secure business IDs:

```
Transaction ID: ECOM-POC-DEV-20250920135400-A1B2-001234-A7F3
                                           |
                                           +-- Unique Instance ID (4 hex chars)

Correlation ID: COR-ECOM-POC-A1B2-12AB34CD-E5F6789A
                             |
                             +-- Unique Instance ID

Session ID: SES-ECOM-POC-A1B2-A1B2C3D4E5F67890-12345678
                                |
                                +-- Salted SHA-256 Hash of User ID (16 hex chars)
```

### Baggage Propagation

Business context automatically flows across all services:

```json
{
  "business.transaction.id": "ECOM-POC-DEV-20250920135400-001234-A7F3",
  "business.correlation.id": "COR-ECOM-POC-12AB34CD-E5F6789A",
  "business.product.code": "ECOM-POC",
  "user.id": "john-doe-123",
  "action.type": "COMPLETE_PROCESSING"
}
```

### Structured Logging

All logs include trace context:

```
2025-09-21 10:30:45.123 [http-nio-8080-exec-1] INFO [1a2b3c4d5e6f7890,9876543210abcdef] c.e.p.c.ProcessingController - Starting complete processing flow for user: john-doe-123
```

## 🏗️ Architecture Deep Dive

### Project Structure

```
src/main/java/com/example/poc/
├── annotation/          # Custom business annotations
│   ├── BusinessOperation.java
│   ├── ActionType.java
│   ├── TraceMethod.java
│   └── ...
├── aspect/             # AspectJ tracing implementation
│   └── DistributedTracingAspect.java
├── config/             # OpenTelemetry configuration
│   └── TracingConfiguration.java
├── controller/         # REST API endpoints
│   └── ProcessingController.java
├── service/            # Business logic with resilience
│   ├── ExternalServiceClient.java
│   └── BusinessContextIdGenerator.java
├── model/              # Request/Response models
│   ├── ProcessingRequest.java
│   └── ProcessingResult.java
└── DistributedTracingResiliencePocApplication.java
```

### Key Design Patterns

#### 1. Aspect-Oriented Programming (AOP)
- **Zero Code Intrusion**: Business code remains clean
- **Automatic Instrumentation**: All methods traced automatically
- **Context Injection**: Business metadata added transparently

#### 2. Decorator Pattern (Resilience4j)
- **Composable Resilience**: Multiple patterns on single method
- **Declarative Configuration**: Annotation-based setup
- **Fallback Strategies**: Graceful degradation

#### 3. Builder Pattern (Business IDs)
- **Hierarchical IDs**: Structured for easy parsing
- **Consistent Format**: Enterprise-ready conventions
- **Correlation Ready**: Perfect for log aggregation

#### 4. **NEW**: Secure Configuration Management
- **Secret Management**: Using a configurable salt for hashing, with clear guidance for using production secret managers (e.g., Vault).
- **Configuration Best Practices**: In-code documentation on using tools like Spring Cloud Config for managing configurations at scale.

### Configuration Highlights

#### Resilience4j Configuration (`application.yml`)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      lambdaService:
        failure-rate-threshold: 50
        sliding-window-size: 10
        wait-duration-in-open-state: 10s

  retry:
    instances:
      lambdaService:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2

  ratelimiter:
    instances:
      lambdaService:
        limit-for-period: 10
        limit-refresh-period: 1s
```

#### **NEW**: Security Configuration (`application.yml`)
```yaml
app:
  security:
    # Salt for hashing user identifiers.
    # IMPORTANT: In a production environment, this value MUST be externalized
    # using a secret manager like HashiCorp Vault, AWS Secrets Manager, or environment variables.
    salt: "a-very-secure-and-random-salt-for-production-use"
```

#### OpenTelemetry Configuration

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling for POC
    baggage:
      correlation:
        fields: user_id,action_type,session_id
      remote-fields: user_id,action_type,business_transaction_id

  otlp:
    tracing:
      endpoint: http://localhost:4317  # ADOT Collector ready
```

## 🔧 Advanced Features

### 1. AWS X-Ray Integration Ready

The POC is configured for seamless AWS X-Ray integration:

```yaml
# For production AWS deployment
management:
  otlp:
    tracing:
      endpoint: http://adot-collector:4317
      headers:
        Authorization: "Bearer ${AWS_TOKEN}"
```

### 2. Business Context Propagation

Custom business context flows automatically:

```java
@BusinessOperation(
    name = "payment-processing",
    category = "financial",
    criticality = "critical",
    sensitive = true
)
public PaymentResult processPayment(PaymentRequest request) {
    // Business context automatically available
    // Tracing happens transparently
    // All resilience patterns active
}
```

### 3. Comprehensive Error Handling

Every pattern includes fallback strategies:

```java
public ProcessingResult fallbackLambdaCall(String userId, String data, Exception ex) {
    logger.warn("Lambda service fallback triggered for user: {} - Reason: {}", userId, ex.getMessage());
    return new ProcessingResult(
        "fallback-result",
        "Using cached/default response due to service unavailability"
    );
}
```

## 📈 Performance Characteristics

### Throughput Capabilities
- **Rate Limiter**: 10 req/sec per service (configurable)
- **Bulkhead**: 3 concurrent requests (semaphore), 4 threads (thread pool)
- **Cache**: 5-minute TTL, 1000 max entries
- **Circuit Breaker**: 50% failure threshold, 10-call window

### Response Time SLAs
- **Fast Operations**: < 100ms (user data fetch)
- **Medium Operations**: 200ms - 1s (EKS processing)
- **Slow Operations**: 500ms - 2s (database operations)
- **Complex Operations**: 400ms - 1.2s (business logic)

## 🚀 Production Deployment

### Docker Ready

```dockerfile
FROM openjdk:17-jre-slim
COPY build/libs/distributed-tracing-resilience-poc-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes Ready

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: distributed-tracing-poc
spec:
  replicas: 3
  selector:
    matchLabels:
      app: distributed-tracing-poc
  template:
    metadata:
      labels:
        app: distributed-tracing-poc
    spec:
      containers:
      - name: app
        image: distributed-tracing-poc:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: OTEL_EXPORTER_OTLP_ENDPOINT
          value: "http://adot-collector:4317"
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
```

### Environment-Specific Configuration

```yaml
# Dev Environment
spring:
  profiles: dev
resilience4j:
  circuitbreaker:
    instances:
      lambdaService:
        failure-rate-threshold: 30  # More lenient in dev

---
# Production Environment
spring:
  profiles: prod
resilience4j:
  circuitbreaker:
    instances:
      lambdaService:
        failure-rate-threshold: 60  # Stricter in production
management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling in production
```

## 🧪 Load Testing Guide

### JMeter Test Plan

```bash
# Simulate realistic load
curl -X POST http://localhost:8080/api/v1/processing/complete-flow \
  -H "Content-Type: application/json" \
  -H "User-ID: load-test-user-$(date +%s)" \
  -d '{"operationId":"load-test-$(date +%s)","data":"load-test-data"}'
```

### Expected Behavior Under Load
1. **Rate Limiter**: Kicks in at 11+ requests/second
2. **Circuit Breaker**: Opens after 50% failure rate
3. **Bulkhead**: Limits concurrent execution to 3
4. **Cache**: Improves performance for repeated requests
5. **Fallbacks**: Graceful degradation when patterns trigger

## 🔍 Troubleshooting Guide

### Common Issues

#### Application Won't Start
```bash
# Check Java version
java -version  # Should be 17+

# Check port availability
lsof -i :8080

# Check logs
./gradlew bootRun --debug
```

#### Endpoints Not Responding
```bash
# Verify application is running
curl http://localhost:8080/actuator/health

# Check logs for errors
tail -f logs/application.log
```

#### Resilience Patterns Not Working
```bash
# Check pattern configuration
curl http://localhost:8080/actuator/circuitbreakers
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.calls

# Verify annotations are processed
curl http://localhost:8080/actuator/beans | grep -i resilience
```

### Log Analysis

Look for these key log patterns:

```bash
# Successful tracing
grep "Trace completed successfully" logs/application.log

# Circuit breaker events
grep "CircuitBreaker.*changed state" logs/application.log

# Retry attempts
grep "Retry.*attempt" logs/application.log

# Rate limiting
grep "RateLimiter.*permit" logs/application.log
```

## 📚 Educational Value

### What This POC Teaches

#### 1. **Enterprise Architecture Patterns**
- Distributed tracing across service boundaries
- Fault tolerance and resilience engineering
- Business context propagation
- Observability and monitoring

#### 2. **Spring Boot Advanced Features**
- AspectJ AOP integration
- Custom auto-configuration
- Actuator custom endpoints
- Profile-based configuration

#### 3. **OpenTelemetry Implementation**
- Custom resource attributes
- Baggage propagation
- OTLP exporter configuration
- AWS X-Ray compatibility

#### 4. **Resilience4j Mastery**
- All 6 resilience patterns
- Pattern composition
- Fallback strategies
- Production configuration

#### 5. **Production Best Practices**
- Structured logging
- Health checks
- Metrics collection
- Error handling
- Security considerations

## 🎯 Next Steps

### Extend the POC

1. **Add Real External Services**
   ```java
   // Replace simulation with real HTTP calls
   @Retryable
   public String callRealLambda(String payload) {
       return restTemplate.postForObject(lambdaUrl, payload, String.class);
   }
   ```

2. **Add Database Integration**
   ```java
   @Repository
   public class ProcessingRepository {
       @CircuitBreaker(name = "database")
       public void saveResult(ProcessingResult result) {
           // Real database operations
       }
   }
   ```

3. **Add Message Queue Integration**
   ```java
   @RabbitListener(queues = "processing.queue")
   @TraceMethod
   public void processMessage(ProcessingRequest request) {
       // Async processing with tracing
   }
   ```

4. **Add Security**
   ```java
   @PreAuthorize("hasRole('PROCESSOR')")
   @BusinessOperation(sensitive = true)
   public ProcessingResult sensitiveOperation() {
       // Secure operations
   }
   ```

## 🏆 Conclusion

This POC demonstrates **enterprise-grade distributed tracing and resilience patterns** in a production-ready Spring Boot application. It showcases:

- ✅ **Complete OpenTelemetry integration** with business context
- ✅ **All 6 Resilience4j patterns** working together seamlessly
- ✅ **Automatic instrumentation** with zero code intrusion
- ✅ **Production monitoring** and observability
- ✅ **AWS X-Ray compatibility** for cloud deployment
- ✅ **Comprehensive testing** and validation

The implementation follows **enterprise best practices** and provides a solid foundation for building resilient, observable distributed systems.

## 📞 Support

For questions about this POC:

1. **Review the logs**: Most issues are captured in application logs
2. **Check configuration**: Verify `application.yml` settings
3. **Test endpoints**: Use the provided curl commands
4. **Monitor metrics**: Use actuator endpoints for insights

---

**Built with ❤️ using Spring Boot, OpenTelemetry, and Resilience4j**

---

## 🧭 OpenTelemetry vs. Micrometer Tracing: Comprehensive Guide

### **Why Both Are Included in This POC**

This POC demonstrates **both OpenTelemetry and Micrometer** working together to show:
1. **When to use each approach** (declarative vs programmatic)
2. **How they complement each other** in real-world scenarios
3. **When they can coexist** and when to choose one

### **🎯 Decision Matrix: When to Use What**

| Scenario | Recommended Approach | Reason |
|----------|---------------------|---------|
| **Standard Spring Boot observability** | Micrometer | Property-driven, autoconfigured, minimal code |
| **Custom span manipulation** | OpenTelemetry | Fine-grained control over span lifecycle |
| **Cross-service baggage** | OpenTelemetry | Direct baggage API, custom propagators |
| **Metrics + Tracing together** | Micrometer | Single abstraction for both |
| **AWS X-Ray integration** | OpenTelemetry | Native OTLP support, custom resource attributes |
| **Simple method instrumentation** | Micrometer | `@Observed` annotation, low code overhead |
| **Complex async workflows** | OpenTelemetry | Context propagation utilities |
| **Multi-backend export** | OpenTelemetry | Custom span processors, flexible exporters |
| **Production standard setup** | Micrometer | Spring autoconfiguration, battle-tested |
| **Advanced tracing patterns** | Hybrid | Micrometer for high-level, OTel for details |

### **🔄 How They Work Together**

```
┌─────────────────────────────────────────────────────┐
│           Application Code                          │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Micrometer @Observed  ──┐    ┌── OpenTelemetry    │
│  (Declarative)           │    │   (Programmatic)   │
│                          ▼    ▼                     │
│              Micrometer Tracing Bridge              │
│                          │                          │
│                          ▼                          │
│              OpenTelemetry SDK                      │
│                          │                          │
│                          ▼                          │
│              OTLP Exporter / X-Ray / Jaeger        │
└─────────────────────────────────────────────────────┘
```

The `micrometer-tracing-bridge-otel` allows Micrometer observations to create OpenTelemetry spans seamlessly.

### **📊 Comparison Table**

| Feature | OpenTelemetry (Programmatic) | Micrometer (Declarative) |
|---------|------------------------------|--------------------------|
| **Setup Complexity** | High (custom SDK beans) | Low (Spring autoconfigure) |
| **Code Intrusion** | Medium (manual span calls) | Minimal (@Observed) |
| **Fine-grained Control** | ✅ Full control | ⚠️ Limited |
| **Async Support** | ✅ Excellent | ⚠️ Basic |
| **Custom Attributes** | ✅ Rich API | ⚠️ Via tags only |
| **Baggage Propagation** | ✅ Direct API | ⚠️ Via config |
| **Spring Integration** | ⚠️ Manual | ✅ Native |
| **Metrics Support** | ❌ Tracing only | ✅ Metrics + Tracing |
| **Learning Curve** | Steep | Gentle |
| **Production Ready** | ✅ (with expertise) | ✅ (out of box) |

### **🚀 Dual Tracing Demo Endpoints**

This POC includes a comprehensive dual tracing demonstration. Access it via:

#### **Programmatic OpenTelemetry Examples**
```bash
# Manual span creation
curl http://localhost:8080/api/v1/tracing/otel/programmatic?operation=data-processing

# Nested spans (parent/child relationships)
curl http://localhost:8080/api/v1/tracing/otel/nested?workflowId=wf-123

# CLIENT span for HTTP calls
curl http://localhost:8080/api/v1/tracing/otel/client?serviceUrl=https://api.example.com

# Async tracing with context propagation
curl http://localhost:8080/api/v1/tracing/otel/async?taskId=task-123

# Custom span builder with fluent API
curl http://localhost:8080/api/v1/tracing/otel/custom?operationId=op-123

# Baggage propagation
curl "http://localhost:8080/api/v1/tracing/otel/baggage?userId=user123&tenantId=tenant456"
```

#### **Declarative Micrometer Examples**
```bash
# Basic @Observed annotation
curl http://localhost:8080/api/v1/tracing/micrometer/basic?input=sample-data

# Micrometer with low-cardinality tags
curl http://localhost:8080/api/v1/tracing/micrometer/tags?operationId=op-123

# Nested Micrometer observations
curl http://localhost:8080/api/v1/tracing/micrometer/nested?workflowId=wf-123
```

#### **Hybrid Approach (Both Together)**
```bash
# Demonstrates both working in harmony
curl http://localhost:8080/api/v1/tracing/hybrid?operationId=hybrid-001
```

#### **Comparison Information**
```bash
# Get detailed comparison
curl http://localhost:8080/api/v1/tracing/comparison

# List all demo endpoints
curl http://localhost:8080/api/v1/tracing/info
```

### **🎓 Micrometer Tracing Demo (Customer Service)**

This repo includes a minimal, annotation-first Micrometer example that coexists with the custom OTel setup:

- **Controller**: `MtrCustomerController` (GET `/api/v1/mtr/customers/{id}`)
- **Service**: `MtrCustomerService`
- **Repository**: `MtrCustomerRepository`
- All annotated with `@Observed(...)` and enabled via `ObservedAspect` bean in `MicrometerObservationConfig`.

**Bridging to OTel**: dependency `io.micrometer:micrometer-tracing-bridge-otel` is added. With this and the aspect, Micrometer observations create OTel spans that appear alongside the custom AOP spans.

**Verify locally:**

```bash
# Start app
./gradlew bootRun

# Hit the Micrometer endpoint
curl http://localhost:8080/api/v1/mtr/customers/42

# Check metrics
curl http://localhost:8080/actuator/metrics | jq
curl "http://localhost:8080/actuator/metrics/http.server.requests" | jq
```

**Tests:**

- **Unit**: `MtrCustomerServiceTest` verifies deterministic mock behavior.
- **Integration**: `MtrTracingIntegrationTest` boots Spring with an in-memory OTel exporter and asserts that controller/service/repository observations produced OTel spans (names/tags like `mtr.customer.*`, `customer.id`).

**Notes:**

- Keep observation names low-cardinality. Favor stable names and tag with `lowCardinalityKeyValues`.
- In production, pick a single control plane (Micrometer or custom OTel). This POC keeps custom OTel primary and uses Micrometer for demo.

### **💡 Recommendations for Production**

#### **Choose Micrometer If:**
- ✅ Your team prefers **property-driven configuration**
- ✅ You need **metrics + tracing** in one abstraction
- ✅ Standard Spring Boot observability is sufficient
- ✅ You want **minimal code changes**
- ✅ Team expertise is more Spring-focused

#### **Choose OpenTelemetry If:**
- ✅ You need **fine-grained span control**
- ✅ **Baggage propagation** is critical
- ✅ You're integrating with **AWS X-Ray** or multi-backend
- ✅ You need **custom resource attributes**
- ✅ Team has OpenTelemetry expertise

#### **Use Hybrid Approach If:**
- ✅ You need **both declarative and programmatic** control
- ✅ Different teams prefer different approaches
- ✅ Migration from one to another is in progress
- ✅ **Complex scenarios** require flexibility

### **🔧 Programmatic Tracing Utilities**

This POC includes `TracingUtils.java` with 20+ helper methods:

```java
// Manual span creation
TracingUtils.withSpan("operation-name", span -> {
    span.setAttribute("custom.attribute", "value");
    // Your business logic
    return result;
});

// Nested spans
TracingUtils.withNestedSpan("parent", "child", (parent, child) -> {
    // Parent and child span context available
    return result;
});

// Async tracing
CompletableFuture<String> future = CompletableFuture.supplyAsync(
    TracingUtils.wrapWithCurrentContext(() -> {
        // Context automatically propagated
        return asyncOperation();
    })
);

// Baggage manipulation
TracingUtils.setBaggage("user.id", userId);
String userId = TracingUtils.getBaggage("user.id");

// Custom span builder
TracingUtils.spanBuilder("custom-operation")
    .setSpanKind(SpanKind.INTERNAL)
    .setAttribute("key", "value")
    .startAndRun(() -> {
        // Your logic
    });
```

### **🌐 HTTP Client Tracing**

Automatic CLIENT span creation for all HTTP calls:

```java
@Bean
public RestTemplate tracedRestTemplate(RestTemplateBuilder builder) {
    return builder
        .interceptors(new TracingRestTemplateInterceptor())
        .build();
}
```

Every HTTP call now automatically creates a CLIENT span with:
- HTTP method, URL, status code
- W3C trace context propagation
- Automatic parent-child relationship

### How to Enable Micrometer Tracing (Alternative Path)

1) Dependencies
- Keep: `org.springframework.boot:spring-boot-starter-actuator`
- Add: `io.micrometer:micrometer-tracing-bridge-otel`
- Optionally remove the manual OpenTelemetry SDK beans to let Spring autoconfigure tracing entirely.

2) Properties (application.yml)
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
    baggage:
      # Keep dotted keys to match code; Micrometer can propagate arbitrary keys
      correlation:
        fields: user.id,action.type,user.session.id,business.transaction.id,business.correlation.id
      remote-fields: user.id,action.type,user.session.id,business.transaction.id,business.correlation.id
  otlp:
    tracing:
      endpoint: http://localhost:4317
```

3) Remove/Adjust Custom SDK
- If you keep the custom `OpenTelemetry` bean, Micrometer will still run, but Spring properties won’t control that custom SDK. For a clean Micrometer-driven setup, remove the custom beans and rely on autoconfiguration.

4) Verify
- Exercise endpoints and confirm traces appear in your backend; validate baggage propagation by inspecting headers and attributes.

5) Notes on baggage naming
- This repo standardizes on dotted baggage keys (e.g., `user.id`, `business.transaction.id`).
- If your org prefers snake_case, change both the code references in `DistributedTracingAspect` and the Micrometer `management.tracing.baggage.*` properties to keep them aligned.

6) Retain OTel as primary
- This POC keeps the custom OpenTelemetry SDK path for maximum control. Micrometer steps above are an optional alternative; don’t enable both control planes at once in production.

### AOP span volume tuning
- Under high load, creating spans for both controllers and services can be noisy and increase cost.
- Strategy options:
  - Annotation-only: Remove layer pointcuts and rely on `@TraceMethod`/`@BusinessOperation` only on critical paths.
  - Layer scoping: Keep controller spans as SERVER and disable service INTERNAL spans for hot paths.
  - Sampling: Reduce sampling rates in prod (already set to 10% in `prod` profile) and add head-based rules.
  - Event-focused: Use span events for fine-grained details instead of separate spans.

### Dependency hygiene (what we streamlined and why)
- Resilience4j: Kept only the Spring Boot 3 starter plus Micrometer binding. Removed individual core modules to avoid duplicate classes/version skew.
- AspectJ weaver: Omitted explicit dependency since `spring-boot-starter-aop` already provides it.
- Logging: Included Logstash JSON encoder but left adoption optional; remove it if you don’t use JSON logging.
- Tracing: Kept both `micrometer-tracing-bridge-otel` and OTLP exporter so you can choose control plane; pick one for production.

### Prioritized recommendations implemented
- Keep OTel SDK as primary control plane; provide Micrometer instructions as an alternative.
- Standardize baggage naming on dotted keys; properties updated accordingly.
- Guard Actuator endpoints with HTTP Basic; only health/info are public (see Security section).
- Streamline dependencies and annotate build with rationale.
- Add inline comments explaining product code sourcing, instanceId purpose, and span volume guidance.

## 🔐 Security for Actuator (NEW)

- Actuator endpoints require HTTP Basic authentication, except `/actuator/health` and `/actuator/info` which remain public by default.
- Set credentials via environment variables (dev defaults shown):

```bash
export ACTUATOR_USER=actuator
export ACTUATOR_PASSWORD=actuator
```

- Production guidance:
  - Use an external identity provider (OIDC/LDAP) and disable in-memory users.
  - Restrict exposed endpoints in the `prod` profile (this repo limits to `health,info`).
  - Consider a separate management port and network policies.

To query authenticated metrics in production:

```bash
curl -u "$ACTUATOR_USER:$ACTUATOR_PASSWORD" http://localhost:8080/actuator/metrics
```

## 🧹 Code Style with Spotless (NEW)

- The build attempts to auto-format code via Spotless and continues without failing if changes are needed.
- To format manually:

```bash
./gradlew spotlessApply
```

## �️ Complete Resilience4j Demo

### **All 6 Patterns Demonstrated**

This POC includes a **comprehensive Resilience4j demonstration** showing all patterns with multiple configurations:

#### **Demo Service & Controller**
- **Service**: `Resilience4jDemoService.java` - Complete pattern implementations
- **Controller**: `Resilience4jDemoController.java` - REST endpoints for testing

### **Pattern 1️⃣: Circuit Breaker**

**Annotations:**
```java
@CircuitBreaker(name = "basicCircuitBreaker", fallbackMethod = "fallbackMethod")
@CircuitBreaker(name = "multiTypeCB", fallbackMethod = "multiTypeFallback")
```

**Endpoints:**
```bash
# Basic circuit breaker
curl http://localhost:8080/api/v1/resilience/circuit-breaker/basic?shouldFail=false

# Multi-type circuit breaker (HTTP + timeout)
curl http://localhost:8080/api/v1/resilience/circuit-breaker/multi-type?shouldFail=false
```

**Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      basicCircuitBreaker:
        failure-rate-threshold: 50        # Open after 50% failures
        sliding-window-size: 10           # Last 10 calls
        wait-duration-in-open-state: 10s  # Wait before half-open

      multiTypeCB:
        failure-rate-threshold: 30
        slow-call-duration-threshold: 2s  # Treat slow calls as failures
        slow-call-rate-threshold: 50
```

**Features Demonstrated:**
- ✅ Failure rate based opening
- ✅ Slow call detection
- ✅ Half-open state transitions
- ✅ Fallback methods
- ✅ Multi-type error handling

### **Pattern 2️⃣: Retry**

**Annotations:**
```java
@Retry(name = "basicRetry")
@Retry(name = "selectiveRetry", fallbackMethod = "retryFallback")
```

**Endpoints:**
```bash
# Basic retry (3 attempts)
curl http://localhost:8080/api/v1/resilience/retry/basic?shouldFail=false

# Selective retry (only on specific exceptions)
curl http://localhost:8080/api/v1/resilience/retry/selective?shouldFail=false
```

**Configuration:**
```yaml
resilience4j:
  retry:
    instances:
      basicRetry:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2  # 1s, 2s, 4s

      selectiveRetry:
        max-attempts: 5
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:
          - java.lang.IllegalArgumentException
```

**Features Demonstrated:**
- ✅ Exponential backoff
- ✅ Selective exception retry
- ✅ Maximum attempts
- ✅ Retry events
- ✅ Fallback on exhaustion

### **Pattern 3️⃣: Rate Limiter**

**Annotations:**
```java
@RateLimiter(name = "basicRateLimiter")
@RateLimiter(name = "waitingRateLimiter")
```

**Endpoints:**
```bash
# Basic rate limiter (fail fast)
curl http://localhost:8080/api/v1/resilience/rate-limiter/basic

# Waiting rate limiter (queue requests)
curl http://localhost:8080/api/v1/resilience/rate-limiter/waiting
```

**Configuration:**
```yaml
resilience4j:
  ratelimiter:
    instances:
      basicRateLimiter:
        limit-for-period: 10               # 10 calls
        limit-refresh-period: 1s           # Per second
        timeout-duration: 0                # Fail immediately

      waitingRateLimiter:
        limit-for-period: 5
        limit-refresh-period: 1s
        timeout-duration: 5s               # Wait up to 5s
```

**Features Demonstrated:**
- ✅ Per-second rate limiting
- ✅ Fail-fast behavior
- ✅ Request queuing
- ✅ Timeout on wait
- ✅ Fair usage enforcement

### **Pattern 4️⃣: Bulkhead**

**Annotations:**
```java
@Bulkhead(name = "semaphoreBulkhead", type = Bulkhead.Type.SEMAPHORE)
@Bulkhead(name = "threadPoolBulkhead", type = Bulkhead.Type.THREADPOOL)
```

**Endpoints:**
```bash
# Semaphore bulkhead (limit concurrent calls)
curl http://localhost:8080/api/v1/resilience/bulkhead/semaphore

# Thread pool bulkhead (isolate threads)
curl http://localhost:8080/api/v1/resilience/bulkhead/threadpool
```

**Configuration:**
```yaml
resilience4j:
  bulkhead:
    instances:
      semaphoreBulkhead:
        max-concurrent-calls: 3            # Only 3 at a time
        max-wait-duration: 0               # Don't wait

  thread-pool-bulkhead:
    instances:
      threadPoolBulkhead:
        max-thread-pool-size: 4
        core-thread-pool-size: 2
        queue-capacity: 2
```

**Features Demonstrated:**
- ✅ Semaphore isolation (shared threads)
- ✅ Thread pool isolation (dedicated threads)
- ✅ Queue capacity limits
- ✅ Resource protection
- ✅ Preventing thread starvation

### **Pattern 5️⃣: Time Limiter**

**Annotation:**
```java
@TimeLimiter(name = "basicTimeLimiter")
```

**Endpoint:**
```bash
# Time limiter for async operations
curl http://localhost:8080/api/v1/resilience/time-limiter?delayMs=1000
```

**Configuration:**
```yaml
resilience4j:
  timelimiter:
    instances:
      basicTimeLimiter:
        timeout-duration: 3s               # Cancel after 3 seconds
        cancel-running-future: true        # Stop the task
```

**Features Demonstrated:**
- ✅ Async operation timeouts
- ✅ Future cancellation
- ✅ CompletableFuture support
- ✅ Preventing hung operations

### **Pattern 6️⃣: Cache**

**Annotation:**
```java
@Cacheable(cacheNames = "demoCache")
@Cacheable(cacheNames = "maxResilienceCache")
```

**Endpoints:**
```bash
# Basic cache
curl http://localhost:8080/api/v1/resilience/cache/basic/user123

# Cache with TTL
curl http://localhost:8080/api/v1/resilience/cache/ttl/user456

# Cache eviction
curl -X DELETE http://localhost:8080/api/v1/resilience/cache/evict/user123
```

**Configuration:**
```yaml
resilience4j:
  cache:
    instances:
      demoCache:
        event-consumer-buffer-size: 10

      maxResilienceCache:
        event-consumer-buffer-size: 100

spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=5m
```

**Features Demonstrated:**
- ✅ Automatic caching
- ✅ TTL-based eviction
- ✅ Manual cache eviction
- ✅ Cache statistics
- ✅ Performance optimization

### **🔄 Combined Patterns**

#### **Triple Pattern (Rate Limiter + Circuit Breaker + Retry)**

**Annotation:**
```java
@RateLimiter(name = "combinedPattern")
@CircuitBreaker(name = "combinedPattern", fallbackMethod = "combinedFallback")
@Retry(name = "combinedPattern")
```

**Endpoint:**
```bash
curl http://localhost:8080/api/v1/resilience/combined/triple?shouldFail=false
```

**Pattern Order:**
1. **Rate Limiter** (outer) - Controls request rate
2. **Circuit Breaker** (middle) - Prevents cascading failures
3. **Retry** (inner) - Retries transient failures

#### **Maximum Resilience (All 6 Patterns)**

**Annotation:**
```java
@RateLimiter(name = "maxResilience")
@CircuitBreaker(name = "maxResilience", fallbackMethod = "maxResilienceFallback")
@Retry(name = "maxResilience")
@Bulkhead(name = "maxResilience")
@TimeLimiter(name = "maxResilience")
@Cacheable(cacheNames = "maxResilienceCache")
```

**Endpoint:**
```bash
curl http://localhost:8080/api/v1/resilience/combined/max?operationId=op-123
```

**Pattern Execution Order:**
1. **Cache** (first check)
2. **Rate Limiter** (control rate)
3. **Circuit Breaker** (fail fast if open)
4. **Bulkhead** (limit concurrency)
5. **Time Limiter** (timeout protection)
6. **Retry** (transient failure handling)

**Features Demonstrated:**
- ✅ Full pattern composition
- ✅ Optimal pattern ordering
- ✅ Comprehensive fault tolerance
- ✅ Production-ready configuration

### **📊 Monitoring Resilience4j Patterns**

```bash
# Circuit Breaker events
curl http://localhost:8080/actuator/circuitbreakerevents

# Retry events
curl http://localhost:8080/actuator/retryevents

# Rate Limiter events
curl http://localhost:8080/actuator/ratelimiterevents

# Bulkhead events
curl http://localhost:8080/actuator/bulkheadevents

# Time Limiter events
curl http://localhost:8080/actuator/timelimiterevents

# Metrics
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.calls
curl http://localhost:8080/actuator/metrics/resilience4j.retry.calls
curl http://localhost:8080/actuator/metrics/resilience4j.ratelimiter.available.permissions
```

### **🎯 Pattern Selection Guide**

| Use Case | Recommended Pattern(s) |
|----------|------------------------|
| **External API calls** | Circuit Breaker + Retry + Time Limiter |
| **Database operations** | Circuit Breaker + Cache |
| **Rate-limited APIs** | Rate Limiter + Retry |
| **Resource-intensive ops** | Bulkhead + Time Limiter |
| **High-frequency reads** | Cache + Circuit Breaker |
| **Mission-critical flows** | All patterns combined |
| **Async operations** | Time Limiter + Bulkhead (thread pool) |
| **Multi-tenant systems** | Rate Limiter + Bulkhead |

## �📌 Update Notes (2025-09-21)

- Upgraded to Spring Boot 3.5.x.
- Refreshed Resilience4j and OpenTelemetry exporter versions.
- Added Spring Security to protect Actuator endpoints.
- Added Spotless plugin for automated code formatting.
