package com.example.poc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for Resilience4jDemoController.
 *
 * <p>Tests REST endpoints for all Resilience4j patterns.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Resilience4jDemoControllerTest {

  @Autowired private MockMvc mockMvc;

  // ==================== Circuit Breaker Tests ====================

  @Test
  void testBasicCircuitBreaker_success() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/api/v1/resilience/circuit-breaker/basic").param("shouldFail", "false"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").exists())
            .andReturn();

    String response = result.getResponse().getContentAsString();
    assertThat(response).contains("Circuit Breaker");
  }

  @Test
  void testBasicCircuitBreaker_fallback() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/circuit-breaker/basic").param("shouldFail", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("FALLBACK")));
  }

  // ==================== Retry Tests ====================

  @Test
  void testBasicRetry_success() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/retry/basic").param("shouldFail", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Retry")));
  }

  @Test
  void testSelectiveRetry_success() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/retry/selective").param("shouldFail", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }

  // ==================== Rate Limiter Tests ====================

  @Test
  void testBasicRateLimiter() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/rate-limiter/basic"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Rate Limiter")));
  }

  @Test
  void testWaitingRateLimiter() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/rate-limiter/waiting"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }

  // ==================== Bulkhead Tests ====================

  @Test
  void testSemaphoreBulkhead() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/bulkhead/semaphore"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Bulkhead")));
  }

  @Test
  void testThreadPoolBulkhead() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/bulkhead/threadpool"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }

  // ==================== Time Limiter Tests ====================

  @Test
  void testTimeLimiter_withinTimeout() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/time-limiter").param("delayMs", "1000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Time Limiter")));
  }

  // ==================== Cache Tests ====================

  @Test
  void testCacheOperation() throws Exception {
    String userId = "testUser123";

    // First call - cache miss
    MvcResult result1 =
        mockMvc
            .perform(get("/api/v1/resilience/cache/basic/{userId}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andReturn();

    // Second call - should hit cache
    MvcResult result2 =
        mockMvc
            .perform(get("/api/v1/resilience/cache/basic/{userId}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andReturn();

    // Results should be identical (cached)
    assertThat(result1.getResponse().getContentAsString())
        .isEqualTo(result2.getResponse().getContentAsString());
  }

  @Test
  void testCacheEviction() throws Exception {
    String userId = "testUser456";

    // Populate cache
    mockMvc.perform(get("/api/v1/resilience/cache/basic/{userId}", userId)).andExpect(status().isOk());

    // Evict cache
    mockMvc
        .perform(delete("/api/v1/resilience/cache/evict/{userId}", userId))
        .andExpect(status().isOk());
  }

  // ==================== Combined Patterns Tests ====================

  @Test
  void testTriplePattern_success() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/combined/triple").param("shouldFail", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Combined Pattern")));
  }

  @Test
  void testMaxResiliencePattern() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/combined/max").param("operationId", "test-op"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(
            jsonPath("$.message")
                .value(org.hamcrest.Matchers.containsString("Maximum Resilience")));
  }

  // ==================== Info Endpoint Tests ====================

  @Test
  void testInfoEndpoint() throws Exception {
    mockMvc
        .perform(get("/api/v1/resilience/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.message").value("Resilience4j Demo Endpoints"))
        .andExpect(jsonPath("$.metadata").exists())
        .andExpect(jsonPath("$.metadata.circuit_breaker_endpoints").exists())
        .andExpect(jsonPath("$.metadata.retry_endpoints").exists())
        .andExpect(jsonPath("$.metadata.rate_limiter_endpoints").exists())
        .andExpect(jsonPath("$.metadata.bulkhead_endpoints").exists())
        .andExpect(jsonPath("$.metadata.time_limiter_endpoints").exists())
        .andExpect(jsonPath("$.metadata.cache_endpoints").exists())
        .andExpect(jsonPath("$.metadata.combined_endpoints").exists());
  }
}
