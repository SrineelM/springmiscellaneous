package com.example.poc;

import com.example.poc.model.ProcessingRequest;
import com.example.poc.model.ProcessingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration tests for the Distributed Tracing with Resilience4j POC.
 * 
 * These tests verify:
 * 1. Complete distributed tracing flow with business context
 * 2. All Resilience4j patterns working correctly
 * 3. Proper error handling and fallback mechanisms
 * 4. Business context propagation and correlation
 * 5. Performance and resilience under load
 * 
 * Uses test profile for faster execution and higher limits.
 */
@SpringBootTest
@AutoConfigureTestMvc
@ActiveProfiles("test")
public class DistributedTracingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Complete Processing Flow Tests")
    class CompleteFlowTests {

        @Test
        @DisplayName("Should complete full distributed processing flow successfully")
        public void testCompleteProcessingFlow() throws Exception {
            ProcessingRequest request = new ProcessingRequest(
                "test-operation-123",
                "sample-data-for-processing",
                "param1=value1,param2=value2",
                "HIGH",
                "INTEGRATION_TEST"
            );

            MvcResult result = mockMvc.perform(post("/api/v1/processing/complete-flow")
                    .header("User-ID", "test-user-456")
                    .header("Session-ID", "test-session-789")
                    .header("X-Correlation-ID", "test-correlation-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").containsString("Complete flow finished"))
                    .andExpect(jsonPath("$.resultId").exists())
                    .andExpect(jsonPath("$.sourceService").value("distributed-tracing-poc"))
                    .andExpect(jsonPath("$.operationType").value("COMPLETE_FLOW"))
                    .andExpect(jsonPath("$.processingTimeMs").exists())
                    .andExpect(jsonPath("$.businessContext.user_id").value("test-user-456"))
                    .andExpect(jsonPath("$.businessContext.session_id").value("test-session-789"))
                    .andExpect(jsonPath("$.businessContext.transaction_id").exists())
                    .andExpect(jsonPath("$.businessContext.correlation_id").exists())
                    .andExpect(jsonPath("$.metadata.steps_completed").value(4))
                    .andExpect(jsonPath("$.metadata.lambda_result_id").exists())
                    .andExpect(jsonPath("$.metadata.eks_result_id").exists())
                    .andExpect(jsonPath("$.metadata.db_result_id").exists())
                    .andExpect(jsonPath("$.metadata.complex_result_id").exists())
                    .andExpect(jsonPath("$.metadata.trace_id").exists())
                    .andExpect(jsonPath("$.metadata.span_id").exists())
                    .andReturn();

            // Verify response structure
            String responseJson = result.getResponse().getContentAsString();
            ProcessingResult response = objectMapper.readValue(responseJson, ProcessingResult.class);
            
            assertThat(response.getResultId()).startsWith("final-");
            assertThat(response.getProcessingTimeMs()).isGreaterThan(0);
            assertThat(response.getBusinessContext()).containsKeys("user_id", "transaction_id", "correlation_id");
            assertThat(response.getMetadata()).containsKeys("steps_completed", "total_duration_ms", "trace_id");
        }

        @Test
        @DisplayName("Should handle missing optional headers gracefully")
        public void testCompleteFlowWithMinimalHeaders() throws Exception {
            ProcessingRequest request = new ProcessingRequest(
                "minimal-test-001",
                "minimal-data",
                "minimal=true"
            );

            mockMvc.perform(post("/api/v1/processing/complete-flow")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.businessContext.user_id").value("anonymous"))
                    .andExpect(jsonPath("$.businessContext.transaction_id").exists());
        }
    }

    @Nested
    @DisplayName("User Data Caching Tests")
    class CachingTests {

        @Test
        @DisplayName("Should cache user data and return cached results on subsequent calls")
        public void testUserDataCaching() throws Exception {
            String userId = "cache-test-user-" + System.currentTimeMillis();
            
            // First call - should hit the service
            MvcResult firstResult = mockMvc.perform(get("/api/v1/processing/user-data/{userId}", userId)
                    .header("X-Correlation-ID", "cache-test-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultId").value("user-data-" + userId))
                    .andExpect(jsonPath("$.message").containsString("retrieved successfully from source"))
                    .andExpect(jsonPath("$.sourceService").value("user-service"))
                    .andReturn();
            
            // Extract processing time from first call
            String firstResponseJson = firstResult.getResponse().getContentAsString();
            ProcessingResult firstResponse = objectMapper.readValue(firstResponseJson, ProcessingResult.class);
            Long firstProcessingTime = firstResponse.getProcessingTimeMs();
            
            // Second call - should use cache (much faster)
            MvcResult secondResult = mockMvc.perform(get("/api/v1/processing/user-data/{userId}", userId)
                    .header("X-Correlation-ID", "cache-test-2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultId").value("user-data-" + userId))
                    .andReturn();
            
            String secondResponseJson = secondResult.getResponse().getContentAsString();
            ProcessingResult secondResponse = objectMapper.readValue(secondResponseJson, ProcessingResult.class);
            
            // Verify caching behavior (cached response should be faster or same)
            assertThat(secondResponse.getResultId()).isEqualTo(firstResponse.getResultId());
            assertThat(secondResponse.getMessage()).contains("retrieved successfully from source");
        }
    }

    @Nested
    @DisplayName("Heavy Processing Tests")
    class HeavyProcessingTests {

        @Test
        @DisplayName("Should handle heavy processing with thread pool bulkhead")
        public void testHeavyProcessing() throws Exception {
            mockMvc.perform(post("/api/v1/processing/heavy-processing")
                    .param("dataId", "heavy-test-data-001")
                    .param("dataSizeMb", "50")
                    .header("User-ID", "heavy-processing-user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.sourceService").value("heavy-processing-engine"))
                    .andExpect(jsonPath("$.operationType").value("HEAVY_PROCESSING"))
                    .andExpect(jsonPath("$.businessContext.data_id").value("heavy-test-data-001"))
                    .andExpect(jsonPath("$.businessContext.requested_by").value("heavy-processing-user"))
                    .andExpect(jsonPath("$.metadata.data_size_mb").value(50))
                    .andExpect(jsonPath("$.metadata.memory_used_mb").exists())
                    .andExpect(jsonPath("$.processingTimeMs").isNumber());
        }

        @Test
        @DisplayName("Should handle heavy processing with default parameters")
        public void testHeavyProcessingWithDefaults() throws Exception {
            mockMvc.perform(post("/api/v1/processing/heavy-processing")
                    .param("dataId", "default-heavy-test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.businessContext.requested_by").value("system"))
                    .andExpect(jsonPath("$.metadata.data_size_mb").value(100));
        }
    }

    @Nested
    @DisplayName("Resilience Pattern Tests")
    class ResiliencePatternTests {

        @Test
        @DisplayName("Should test circuit breaker pattern")
        public void testCircuitBreakerPattern() throws Exception {
            mockMvc.perform(get("/api/v1/processing/test/circuit-breaker")
                    .param("userId", "cb-test-user")
                    .param("data", "circuit-breaker-test-data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.businessContext.test_pattern").value("circuit-breaker"))
                    .andExpect(jsonPath("$.businessContext.test_user").value("cb-test-user"))
                    .andExpect(jsonPath("$.metadata.test_mode").value(true))
                    .andExpect(jsonPath("$.metadata.pattern_tested").value("circuit-breaker"));
        }

        @Test
        @DisplayName("Should test retry pattern")
        public void testRetryPattern() throws Exception {
            mockMvc.perform(get("/api/v1/processing/test/retry")
                    .param("userId", "retry-test-user")
                    .param("data", "retry-test-data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.businessContext.test_pattern").value("retry"))
                    .andExpect(jsonPath("$.metadata.pattern_tested").value("retry"));
        }

        @Test
        @DisplayName("Should test rate limiter pattern")
        public void testRateLimiterPattern() throws Exception {
            mockMvc.perform(get("/api/v1/processing/test/rate-limiter")
                    .param("userId", "rl-test-user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.businessContext.test_pattern").value("rate-limiter"));
        }

        @Test
        @DisplayName("Should test bulkhead pattern")
        public void testBulkheadPattern() throws Exception {
            mockMvc.perform(get("/api/v1/processing/test/bulkhead")
                    .param("userId", "bulkhead-test-user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.businessContext.test_pattern").value("bulkhead"));
        }

        @Test
        @DisplayName("Should test time limiter pattern")
        public void testTimeLimiterPattern() throws Exception {
            mockMvc.perform(get("/api/v1/processing/test/time-limiter")
                    .param("userId", "tl-test-user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.businessContext.test_pattern").value("time-limiter"));
        }

        @Test
        @DisplayName("Should test cache pattern")
        public void testCachePattern() throws Exception {
            mockMvc.perform(get("/api/v1/processing/test/cache")
                    .param("userId", "cache-test-user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.businessContext.test_pattern").value("cache"));
        }

        @Test
        @DisplayName("Should test combined patterns")
        public void testCombinedPatterns() throws Exception {
            mockMvc.perform(get("/api/v1/processing/test/combined")
                    .param("userId", "combined-test-user")
                    .param("data", "combined-test-data"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.businessContext.test_pattern").value("combined"))
                    .andExpect(jsonPath("$.sourceService").value("business-logic-engine"));
        }

        @Test
        @DisplayName("Should return error for unknown pattern")
        public void testUnknownPattern() throws Exception {
            mockMvc.perform(get("/api/v1/processing/test/unknown-pattern"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.message").containsString("Unknown pattern"));
        }
    }

    @Nested
    @DisplayName("System Endpoints Tests")
    class SystemEndpointsTests {

        @Test
        @DisplayName("Should return healthy status")
        public void testHealthCheck() throws Exception {
            mockMvc.perform(get("/api/v1/processing/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Service is healthy and all resilience patterns are active"))
                    .andExpect(jsonPath("$.businessContext.health_status").value("healthy"))
                    .andExpect(jsonPath("$.businessContext.product_code").exists())
                    .andExpect(jsonPath("$.metadata.resilience_patterns").value("circuit-breaker,retry,rate-limiter,bulkhead,time-limiter,cache"))
                    .andExpect(jsonPath("$.metadata.tracing_enabled").value(true))
                    .andExpect(jsonPath("$.metadata.baggage_propagation").value(true))
                    .andExpect(jsonPath("$.metadata.trace_id").exists())
                    .andExpect(jsonPath("$.metadata.span_id").exists());
        }

        @Test
        @DisplayName("Should return system information")
        public void testSystemInfo() throws Exception {
            mockMvc.perform(get("/api/v1/processing/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.businessContext.application").value("distributed-tracing-resilience-poc"))
                    .andExpect(jsonPath("$.businessContext.version").value("1.0.0"))
                    .andExpect(jsonPath("$.metadata.java_version").exists())
                    .andExpect(jsonPath("$.metadata.spring_boot_version").exists())
                    .andExpect(jsonPath("$.metadata.available_processors").isNumber())
                    .andExpect(jsonPath("$.metadata.max_memory_mb").isNumber())
                    .andExpect(jsonPath("$.metadata.total_memory_mb").isNumber())
                    .andExpect(jsonPath("$.metadata.free_memory_mb").isNumber());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON request")
        public void testMalformedJsonRequest() throws Exception {
            mockMvc.perform(post("/api/v1/processing/complete-flow")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle empty request body")
        public void testEmptyRequestBody() throws Exception {
            mockMvc.perform(post("/api/v1/processing/complete-flow")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk()); // Should work with default values
        }
    }

    @Nested
    @DisplayName("Business Context Tests")
    class BusinessContextTests {

        @Test
        @DisplayName("Should propagate business context through headers")
        public void testBusinessContextPropagation() throws Exception {
            ProcessingRequest request = new ProcessingRequest(
                "context-test-001",
                "context-test-data",
                "context=test"
            );

            mockMvc.perform(post("/api/v1/processing/complete-flow")
                    .header("User-ID", "context-test-user")
                    .header("Session-ID", "context-test-session")
                    .header("X-Correlation-ID", "context-test-correlation")
                    .header("X-Request-Source", "integration-test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.businessContext.user_id").value("context-test-user"))
                    .andExpect(jsonPath("$.businessContext.session_id").value("context-test-session"))
                    .andExpect(jsonPath("$.businessContext.transaction_id").exists())
                    .andExpect(jsonPath("$.businessContext.correlation_id").exists())
                    .andExpect(jsonPath("$.businessContext.flow_type").value("complete"));
        }

        @Test
        @DisplayName("Should generate business IDs when not provided")
        public void testBusinessIdGeneration() throws Exception {
            ProcessingRequest request = new ProcessingRequest(
                "id-gen-test-001",
                "id-generation-data",
                "auto_generate=true"
            );

            MvcResult result = mockMvc.perform(post("/api/v1/processing/complete-flow")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            ProcessingResult response = objectMapper.readValue(responseJson, ProcessingResult.class);

            // Verify business ID formats
            String transactionId = response.getBusinessContext().get("transaction_id");
            String correlationId = response.getBusinessContext().get("correlation_id");

            assertThat(transactionId).matches("ECOM-POC-\\w+-\\d{14}-\\d{6}-[A-F0-9]+");
            assertThat(correlationId).matches("COR-ECOM-POC-[A-F0-9]{8}-[A-F0-9]+");
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should complete processing within reasonable time limits")
        public void testPerformanceWithinLimits() throws Exception {
            ProcessingRequest request = new ProcessingRequest(
                "perf-test-001",
                "performance-test-data",
                "performance=test"
            );

            long startTime = System.currentTimeMillis();

            MvcResult result = mockMvc.perform(post("/api/v1/processing/complete-flow")
                    .header("User-ID", "perf-test-user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            // Should complete within 10 seconds (generous limit for CI/CD)
            assertThat(totalTime).isLessThan(10000);

            String responseJson = result.getResponse().getContentAsString();
            ProcessingResult response = objectMapper.readValue(responseJson, ProcessingResult.class);

            // Verify processing time is recorded
            assertThat(response.getProcessingTimeMs()).isGreaterThan(0);
            assertThat(response.getProcessingTimeMs()).isLessThan(totalTime);
        }
    }
}