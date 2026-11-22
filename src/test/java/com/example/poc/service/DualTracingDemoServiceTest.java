package com.example.poc.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for DualTracingDemoService.
 *
 * <p>Tests both OpenTelemetry (programmatic) and Micrometer (declarative) tracing approaches.
 */
@SpringBootTest
class DualTracingDemoServiceTest {

  @Autowired private DualTracingDemoService service;

  // ==================== OpenTelemetry Programmatic Tests ====================

  @Test
  void programmaticOtelTracing_shouldSucceed() throws Exception {
    // Act
    String result = service.programmaticOtelTracing("test-operation");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("Programmatic OpenTelemetry Tracing");
    assertThat(result).contains("test-operation");
  }

  @Test
  void nestedSpansExample_shouldCreateHierarchy() throws Exception {
    // Act
    String result = service.nestedSpansExample("workflow-123");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("Nested Spans Example");
    assertThat(result).contains("workflow-123");
  }

  @Test
  void clientSpanExample_shouldTraceHttpCall() throws Exception {
    // Act
    String result = service.clientSpanExample("https://api.example.com/test");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("CLIENT Span Example");
  }

  @Test
  void asyncTracingExample_shouldPropagateContext() throws Exception {
    // Act
    CompletableFuture<String> future = service.asyncTracingExample("async-task-001");

    // Assert
    String result = future.get();
    assertThat(result).isNotNull();
    assertThat(result).contains("Async Tracing Example");
    assertThat(result).contains("async-task-001");
  }

  @Test
  void customSpanBuilderExample_shouldCreateCustomSpan() throws Exception {
    // Act
    String result = service.customSpanBuilderExample("custom-op-001");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("Custom Span Builder Example");
    assertThat(result).contains("custom-op-001");
  }

  @Test
  void baggageExample_shouldPropagateBaggage() {
    // Act
    String result = service.baggageExample("user-123", "tenant-456");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("Baggage Example");
    assertThat(result).contains("user-123");
    assertThat(result).contains("tenant-456");
  }

  // ==================== Micrometer Declarative Tests ====================

  @Test
  void basicMicrometerTracing_shouldCreateObservation() {
    // Act
    String result = service.basicMicrometerTracing("sample-input");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("Basic Micrometer Tracing");
    assertThat(result).contains("sample-input");
  }

  @Test
  void micrometerWithTags_shouldIncludeTags() {
    // Act
    String result = service.micrometerWithTags("tagged-op-001");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("Micrometer with Tags");
    assertThat(result).contains("tagged-op-001");
  }

  @Test
  void nestedMicrometerObservations_shouldCreateNesting() {
    // Act
    String result = service.nestedMicrometerObservations("nested-wf-001");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("Nested Micrometer Observations");
    assertThat(result).contains("nested-wf-001");
  }

  // ==================== Hybrid Approach Test ====================

  @Test
  void hybridTracingExample_shouldCombineBothApproaches() throws Exception {
    // Act
    String result = service.hybridTracingExample("hybrid-op-001");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("Hybrid Tracing Example");
    assertThat(result).contains("hybrid-op-001");
  }

  // ==================== Comparison Information Test ====================

  @Test
  void getTracingComparison_shouldReturnComprehensiveInfo() {
    // Act
    Map<String, String> comparison = service.getTracingComparison();

    // Assert
    assertThat(comparison).isNotNull();
    assertThat(comparison).isNotEmpty();
    assertThat(comparison).containsKeys(
        "opentelemetry_approach",
        "micrometer_approach",
        "when_to_use_opentelemetry",
        "when_to_use_micrometer",
        "hybrid_approach",
        "key_difference",
        "recommendation"
    );

    // Verify content
    assertThat(comparison.get("opentelemetry_approach"))
        .contains("Programmatic")
        .contains("fine-grained control");
    assertThat(comparison.get("micrometer_approach"))
        .contains("Declarative")
        .contains("@Observed");
    assertThat(comparison.get("when_to_use_opentelemetry"))
        .contains("custom span manipulation");
    assertThat(comparison.get("when_to_use_micrometer"))
        .contains("standard Spring Boot");
  }

  // ==================== Multiple Calls Test ====================

  @Test
  void programmaticOtelTracing_shouldHandleMultipleCalls() throws Exception {
    // Act - Call multiple times
    String result1 = service.programmaticOtelTracing("operation-1");
    String result2 = service.programmaticOtelTracing("operation-2");
    String result3 = service.programmaticOtelTracing("operation-3");

    // Assert - Each call should succeed
    assertThat(result1).contains("operation-1");
    assertThat(result2).contains("operation-2");
    assertThat(result3).contains("operation-3");
  }

  @Test
  void basicMicrometerTracing_shouldHandleMultipleCalls() {
    // Act - Call multiple times
    String result1 = service.basicMicrometerTracing("input-1");
    String result2 = service.basicMicrometerTracing("input-2");
    String result3 = service.basicMicrometerTracing("input-3");

    // Assert - Each call should succeed
    assertThat(result1).contains("input-1");
    assertThat(result2).contains("input-2");
    assertThat(result3).contains("input-3");
  }
}
