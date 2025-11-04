package com.example.poc.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for DualTracingDemoController.
 *
 * <p>Tests REST endpoints demonstrating OpenTelemetry and Micrometer tracing.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DualTracingDemoControllerTest {

  @Autowired private MockMvc mockMvc;

  // ==================== OpenTelemetry Programmatic Tests ====================

  @Test
  void testProgrammaticOtel() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/otel/programmatic").param("operation", "test-operation"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.operationType").value("OTEL_PROGRAMMATIC"))
        .andExpect(jsonPath("$.businessContext['tracing.framework']").value("OpenTelemetry"));
  }

  @Test
  void testNestedSpans() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/otel/nested").param("workflowId", "wf-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.operationType").value("OTEL_NESTED"));
  }

  @Test
  void testClientSpan() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/tracing/otel/client").param("serviceUrl", "https://api.example.com"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.operationType").value("OTEL_CLIENT"));
  }

  @Test
  void testAsyncTracing() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/otel/async").param("taskId", "async-task-001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.operationType").value("OTEL_ASYNC"));
  }

  @Test
  void testCustomSpanBuilder() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/otel/custom").param("operationId", "op-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.operationType").value("OTEL_CUSTOM"));
  }

  @Test
  void testBaggage() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/tracing/otel/baggage")
                .param("userId", "user123")
                .param("tenantId", "tenant456"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.operationType").value("OTEL_BAGGAGE"));
  }

  // ==================== Micrometer Declarative Tests ====================

  @Test
  void testBasicMicrometer() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/micrometer/basic").param("input", "sample-data"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.operationType").value("MICROMETER_BASIC"))
        .andExpect(jsonPath("$.businessContext['tracing.framework']").value("Micrometer"));
  }

  @Test
  void testMicrometerWithTags() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/micrometer/tags").param("operationId", "op-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.operationType").value("MICROMETER_TAGS"));
  }

  @Test
  void testNestedMicrometer() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/micrometer/nested").param("workflowId", "wf-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.operationType").value("MICROMETER_NESTED"));
  }

  // ==================== Hybrid Approach Test ====================

  @Test
  void testHybridTracing() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/hybrid").param("operationId", "hybrid-001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.operationType").value("HYBRID"))
        .andExpect(
            jsonPath("$.businessContext['tracing.framework']")
                .value("OpenTelemetry + Micrometer"));
  }

  // ==================== Comparison Endpoint Test ====================

  @Test
  void testTracingComparison() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/comparison"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.opentelemetry_approach").exists())
        .andExpect(jsonPath("$.micrometer_approach").exists())
        .andExpect(jsonPath("$.when_to_use_opentelemetry").exists())
        .andExpect(jsonPath("$.when_to_use_micrometer").exists())
        .andExpect(jsonPath("$.hybrid_approach").exists());
  }

  // ==================== Info Endpoint Test ====================

  @Test
  void testTracingInfo() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.message").value("Distributed Tracing Demo Endpoints"))
        .andExpect(jsonPath("$.metadata.otel_endpoints").exists())
        .andExpect(jsonPath("$.metadata.micrometer_endpoints").exists())
        .andExpect(jsonPath("$.metadata.hybrid_endpoints").exists())
        .andExpect(jsonPath("$.metadata.comparison_endpoint").exists())
        .andExpect(jsonPath("$.metadata.recommendation").exists())
        .andExpect(jsonPath("$.metadata.best_practice").exists());
  }

  // ==================== Parameter Validation Tests ====================

  @Test
  void testProgrammaticOtel_withDefaultParameters() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/otel/programmatic"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }

  @Test
  void testNestedSpans_withDefaultParameters() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/otel/nested"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }

  @Test
  void testBasicMicrometer_withDefaultParameters() throws Exception {
    mockMvc
        .perform(get("/api/v1/tracing/micrometer/basic"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }
}
