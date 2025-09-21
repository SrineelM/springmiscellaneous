# Architecture Review – Distributed Tracing & Resilience POC

Date: 2025-09-21

## Overview

This POC demonstrates a clean Spring Boot architecture with comprehensive observability and resilience baked in. It uses:
- Spring Boot (controllers/services/config)
- Custom AOP aspect for distributed tracing and business context
- OpenTelemetry SDK for tracing (with OTLP export)
- Resilience4j for circuit breaker, retry, rate limiter, bulkhead (semaphore + thread-pool), time-limiter, and cache
- Rich business-aware ID generation

## Strengths

- Clear layering and responsibilities (controller orchestration, service integration, aspect cross-cutting)
- Business-domain annotations that make intent explicit (@BusinessOperation, @TraceMethod, @ActionType, @FeatureName)
- End-to-end resilience with realistic fallbacks and configurations
- Rich trace attributes and MDC integration for logs
- Scalable and privacy-preserving ID strategy (instanceId + salted SHA-256)
- Excellent configuration comments and README guidance

## Risks and Gaps

1) Tracing stack alignment
- The project uses the OpenTelemetry SDK directly while also declaring Micrometer tracing dependencies and Spring `management.tracing.*` configuration. This is valid, but keep in mind Spring’s Micrometer baggage/sampling properties won’t automatically govern a custom OTel SDK configuration. Pick one as the source of truth to avoid confusion.

2) Tracer bean DI
- The AOP aspect depends on `io.opentelemetry.api.trace.Tracer`. Ensure a `Tracer` bean is exposed (derived from the `OpenTelemetry` bean) to avoid relying on global registration. This keeps tracing injectable and testable.

3) Thread-pool bulkhead + async pattern
- For methods annotated with `@Bulkhead(type = THREADPOOL)`/`@TimeLimiter`, prefer letting Resilience4j control execution (don’t call `CompletableFuture.supplyAsync` yourself). This ensures isolation and time limiting use the pattern’s executor rather than the common pool.

4) Baggage key consistency
- Spring’s Micrometer baggage config uses snake_case by default in examples, while the code uses dotted keys (`user.id`, `business.transaction.id`). If you adopt Micrometer for baggage propagation, align naming or continue to rely exclusively on OTel Baggage and document the choice.

5) Actuator exposure in prod
- The current configuration exposes many actuator endpoints and shows full health details. Great for dev; restrict in prod and add security.

6) Dependency overlap
- Minor redundancy (e.g., AOP weaver provided by starter vs explicit). Consider trimming when you finalize the stack.

## Recommendations (prioritized)

1. Provide a `Tracer` bean (`@Bean Tracer`) from the `OpenTelemetry` bean; inject into the aspect.
2. Choose a single tracing control plane:
   - Option A: Keep custom OTel SDK; remove Micrometer baggage config to avoid confusion.
   - Option B: Use Micrometer Tracing autoconfiguration; remove manual SDK and configure via properties.
3. For thread-pool bulkhead/time-limiter methods, let Resilience4j run the supplier on its executor; avoid manual `supplyAsync`.
4. Lock down actuator in prod (only expose health/info, secure the rest via HTTP Basic or OIDC).
5. Trim duplicate dependencies and adopt BOMs for OTel/Resilience4j alignment.
6. Add focused tests for ID formats, aspect attributes/MDC, and key resilience paths.

## Why OpenTelemetry (OTel) here

OTel provides first-class, vendor-neutral APIs and far-reaching customization:
- Full control over samplers, processors, exporters, and resource attributes
- Standard W3C Trace Context + Baggage propagation out of the box
- Rich, low-level Span/Attribute/Event APIs for precise modeling of business semantics
- Broad ecosystem support (collectors, backends, auto-instrumentation)

Micrometer Tracing is excellent when you want property-driven Spring autoconfiguration and uniform Micrometer abstractions. In this POC, the custom OTel SDK setup gives maximal flexibility to encode business semantics and to fine-tune export/processors.

## Micrometer Tracing – migration/enablement steps (summary)

See README for step-by-step instructions on enabling Micrometer Tracing as an alternative to the custom OTel SDK.
