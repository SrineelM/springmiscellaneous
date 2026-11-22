package com.example.poc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Shared meta-annotation for integration tests.
 *
 * <p>Educational value: By activating the 'test' profile across all SpringBootTests, we ensure
 * tests merge with main configuration instead of shadowing it, avoid attempting external OTLP
 * network calls (localhost:4317), and ensure test isolation for GlobalOpenTelemetry registration.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
public @interface PocTest {}
