# Configuration Management Guide

## 1. Overview & Architecture

In local development and Proofs of Concept (POC), a consolidated `application.yml` file allows quick inspection of all configuration options across environments. However, enterprise production deployments demand strict externalization, automated secret rotation, dynamic configuration refreshes, and clear profile isolation.

## 2. Secrets Management & Hardening

### The Salt (`app.security.salt`)
- **POC / Dev**: Defaults to `dev-only-salt` for developer ergonomics.
- **Production (`prod` profile)**: **Must** be provided via the `APP_SECURITY_SALT` environment variable or enterprise secret store (HashiCorp Vault, AWS Secrets Manager, Google Secret Manager). The application enforces this at startup via `BusinessContextIdGenerator` validation: if active profile is `prod` and the salt is missing or set to the default value, the Spring context refuses to start.

### Actuator Credentials (`app.actuator.username` and `app.actuator.password`)
- **Endpoints Protected**: All management endpoints under `/actuator/**` except `/actuator/health` and `/actuator/info` are protected by HTTP Basic authentication.
- **POC / Dev / Test**: Defaults to `actuator` / `actuator`.
- **Production**: Configured via `ACTUATOR_USER` and `ACTUATOR_PASSWORD`. In production, the password **must not** be blank or default.

## 3. Profiles Reference

| Profile | Purpose | Circuit Breaker Wait | Sampling Rate | Logging Verbosity |
|---------|---------|---------------------|---------------|-------------------|
| `dev`   | Local development & debugging | 10s | 100% (1.0) | TRACE / DEBUG |
| `test`  | Unit & integration tests | 1s (fast recovery) | 100% (in-memory) | INFO (no OTLP export) |
| `prod`  | Hardened production deployment | 15s | 10% (0.1) | INFO |
| `virtual` | Opt-in Java 21 Virtual Threads | Inherits base/profile | Inherits | Virtual thread executor |

## 4. Key Property Reference

### Server & Tomcat
- `server.tomcat.threads.max`: Maximum worker threads (default: 200). Note: `server.tomcat.max-threads` was deprecated/removed in modern Spring Boot in favor of `threads.max`.
- `server.tomcat.accept-count`: Maximum queue length for incoming connection requests (default: 100).

### Tracing & OpenTelemetry
- `management.tracing.sampling.probability`: Sampling probability between `0.0` (0%) and `1.0` (100%).
- `management.otlp.tracing.endpoint`: Target OTLP gRPC endpoint (port 4317). Used by `TracingConfiguration` when building the custom `OtlpGrpcSpanExporter`. In test profiles, this is redirected to `http://localhost:0` to guarantee no external network calls occur.
- `management.endpoints.web.exposure.include`: Actuator endpoints exposed over HTTP. Includes `health,info,metrics,circuitbreakers,ratelimiters,retries,bulkheads,timelimiters,caches,httpexchanges`.

### Caching
- `spring.cache.type`: Configured as `caffeine`.
- `spring.cache.caffeine.spec`: Global specification (`maximumSize=1000,expireAfterWrite=300s`). Custom cache names (such as `userDataCache`, `demoCache`) use this spec.

### Resilience4j Pattern Properties
- `resilience4j.circuitbreaker`: Configures sliding window (COUNT_BASED), failure rate threshold (50%), minimum calls (5), and wait duration in open state.
- `resilience4j.retry`: Configures max-attempts (3), wait-duration (1s), exponential backoff multiplier (2).
- `resilience4j.ratelimiter`: Configures request limits per period (e.g. 10 requests per 1s).
- `resilience4j.bulkhead`: Semaphore-based concurrency limiter (max concurrent calls: 5).
- `resilience4j.thread-pool-bulkhead`: Thread-pool based isolation for async operations returning `CompletableFuture`.
- `resilience4j.timelimiter`: Timeout bounds for asynchronous tasks.
