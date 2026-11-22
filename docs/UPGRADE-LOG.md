# Upgrade Log: Distributed Tracing & Resilience4j POC

## 1. Baseline Summary (Initial State)

- **Repository**: Spring Boot POC for Distributed Tracing & Resilience4j (`com.example.poc`).
- **Build System**: Gradle 9.7.1 wrapper (`gradle/wrapper/gradle-wrapper.properties`).
- **Initial Target Language**: Java 17 configured via `sourceCompatibility = JavaVersion.VERSION_17`.
- **Spring Boot Version**: 3.5.0 (`org.springframework.boot` plugin).
- **Core Dependencies**: Resilience4j 2.3.0 (`resilience4j-spring-boot3`, `resilience4j-micrometer`), Caffeine, Logstash logback encoder 8.0.
- **Tracing Implementation**: Dual tracing with programmatic OpenTelemetry SDK 1.40.0 and declarative Micrometer `@Observed` (`micrometer-tracing-bridge-otel`).
- **Actuator & Security**: Spring Security securing `/actuator/**` with HTTP Basic, public `/actuator/health` and `/actuator/info`.
- **Configuration**: Monolithic `src/main/resources/application.yml` with `dev`, `prod`, `test` profile documents.
- **Spotless**: Plugin 6.25.0 configured with all `*Check` tasks disabled, auto-formatting wired into `build`.
- **Test Infrastructure**: 8 test classes (7 in `src/test/java`, 1 resource `src/test/resources/application.yml`).
- **Unused Dependencies**: `org.testcontainers:junit-jupiter` and unverified `org.hamcrest:hamcrest`.
- **Baseline Test Execution**: Running `./gradlew clean test` on unchanged repository failed.
- **Baseline Test Counts**: 52 tests completed, 28 failed, 1 skipped.
- **Primary Failure Causes**: (1) `src/test/resources/application.yml` completely shadows main `application.yml`, eliminating Resilience4j/Security/Actuator configurations. (2) `GlobalOpenTelemetry` collision across test suites lacking `@ActiveProfiles("test")`. (3) Disabled test in `TracingIntegrationTest`.

## 2. Baseline Test Run Proof

```
> Task :test
DualTracingDemoControllerTest > testTracingComparison() FAILED
Resilience4jDemoControllerTest > 14 tests FAILED (all endpoints returning 401/404 due to config shadow)
DualTracingDemoServiceTest > 13 tests FAILED (GlobalOpenTelemetry already registered)
52 tests completed, 28 failed, 1 skipped
BUILD FAILED in 53s
```

## 3. Verified Dependency Versions

| Component | Target Version | Release / Verification Date | Verification Source |
|-----------|----------------|----------------------------|---------------------|
| OpenJDK Toolchain | 21 | 2026-11-23 | Eclipse Adoptium / Temurin LTS |
| Gradle Wrapper | 9.7.1 | 2026-08-19 | Gradle Services (`gradle-9.7.1-bin.zip`) |
| Spring Boot (Stage 1) | 3.5.16 | 2026-06-30 | Maven Central / Spring IO Release Index |
| Spring Dependency Management | 1.1.7 | 2026-11-23 | Maven Central / Spring Plugins |
| Spotless | 7.0.2 | 2026-11-23 | Gradle Plugin Portal |
| Google Java Format | 1.25.2 | 2026-11-23 | Maven Central (JDK 21 language syntax) |
| Resilience4j (Stage 1) | 2.3.0 | 2026-11-23 | Maven Central |
| Logstash Logback Encoder | 8.0 | 2026-11-23 | Maven Central |

## 4. Behavior Changes Log

- None yet (Baseline).
