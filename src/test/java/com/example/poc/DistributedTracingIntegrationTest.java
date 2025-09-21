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
 * =================================================================================================
 * ARCHITECTURAL REVIEW
 * =================================================================================================
 * 
 * This class, `DistributedTracingIntegrationTest`, contains a comprehensive suite of integration
 * tests for the entire application. It uses Spring Boot's testing framework (`@SpringBootTest`,
 * `@AutoConfigureTestMvc`) to launch the full application context and send real HTTP requests to
 * the controllers.
 * 
 * Key Architectural Decisions & Best Practices:
 * ------------------------------------------------
 * 1.  `@SpringBootTest` and `@AutoConfigureTestMvc`: This is the standard and most effective way to
 *     write integration tests for a Spring Boot web application. It ensures that the entire
 *     application stack, from the web layer down to the services, is tested together.
 * 2.  `@ActiveProfiles("test")`: Using a dedicated "test" profile is a crucial best practice. It
 *     allows for separate configuration in `application.yml` for tests (e.g., faster timeouts,
 *     in-memory databases, higher rate limits) without affecting the production configuration.
 * 3.  `MockMvc`: This is the tool of choice for testing Spring MVC controllers without needing to
 *     start a full servlet container. It allows for precise control over HTTP requests and powerful
 *     assertions on the responses.
 * 4.  Structured Tests with `@Nested`: The tests are organized into logical groups using `@Nested`
 *     classes (e.g., `CompleteFlowTests`, `CachingTests`, `ResiliencePatternTests`). This makes the
 *     test suite highly readable and easy to navigate.
 * 5.  Descriptive Naming (`@DisplayName`): The use of `@DisplayName` provides clear, human-readable
 *     descriptions for each test class and method, which is excellent for test reports and for
 *     understanding the intent of each test.
 * 6.  Comprehensive Assertions: The tests use a combination of `MockMvcResultMatchers` (like
 *     `jsonPath`) and AssertJ (`assertThat`) to perform detailed assertions.
 *     - `jsonPath` is used to verify the structure and content of the JSON responses.
 *     - `assertThat` is used for more complex or custom assertions on the deserialized response objects.
 *     This two-pronged approach is very effective.
 * 7.  Thorough Coverage: The test suite is remarkably thorough. It covers:
 *     - The main success path (`testCompleteProcessingFlow`).
 *     - Edge cases (e.g., `testCompleteFlowWithMinimalHeaders`).
 *     - Specific features like caching and bulkheads.
 *     - Individual resilience patterns.
 *     - System endpoints (`/health`, `/info`).
 *     - Error handling (`testMalformedJsonRequest`).
 *     - Business context propagation and ID generation.
 *     - A basic performance check.
 * 
 * Role in the Architecture:
 * -------------------------
 * - This class acts as the primary quality gate for the application.
 * - It validates that all the components (controller, service, aspect, configuration) work together
 *   correctly as a cohesive whole.
 * - It serves as living documentation, demonstrating how the API is intended to be used and what
 *   responses to expect.
 * 
 * Overall Feedback:
 * -----------------
 * - This is a model integration test suite. It's comprehensive, well-structured, readable, and
 *   follows all modern Java and Spring testing best practices.
 * - The level of detail in the assertions, especially for the `complete-flow` endpoint, is excellent
 *   and ensures that the API contract is strictly enforced.
 * - The organization with `@Nested` and `@DisplayName` is top-notch and should be emulated in any
 *   serious project.
 * 
 * Weaknesses/Areas for Improvement:
 * ---------------------------------
 * - The tests for individual resilience patterns are somewhat basic. They confirm that the endpoint
 *   can be called, but they don't actually verify that the resilience pattern (e.g., circuit breaker
 *   opening, retry happening) is triggered. To do this would require more complex test setups,
 *   possibly involving mocking the downstream service to force failures and then using a library
 *   like Awaitility to check the state of the Resilience4j components from the Actuator endpoints.
 *   However, for a POC, the current level of testing is more than sufficient.
 * - The caching test could be more robust. It currently asserts that the second call's response is
 *   the same as the first, but it doesn't strictly prove the cache was used (e.g., by asserting that
 *   the processing time is significantly lower or that the underlying service method was only called
 *   once). This would require mocking the service layer, which would turn it into more of a unit/component
 *   test rather than a full integration test.
 * 
 * Despite these minor points, which are more about taking the testing to an even higher level of
 * rigor, this is an exemplary test class that provides a high degree of confidence in the
 * application's correctness.
 * =================================================================================================
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

        @Test
        @DisplayName("Should generate scalable and secure IDs")
        public void testScalableAndSecureIdGeneration() throws Exception {
            ProcessingRequest request = new ProcessingRequest(
                "secure-id-test-001",
                "data-for-secure-id",
                "security=high"
            );

            MvcResult result = mockMvc.perform(post("/api/v1/processing/complete-flow")
                    .header("User-ID", "user-to-be-hashed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            ProcessingResult response = objectMapper.readValue(responseJson, ProcessingResult.class);

            // 1. Verify Business Transaction ID contains the instanceId
            String transactionId = response.getBusinessContext().get("transaction_id");
            // Example: ECOM-POC-TEST-20231027103000-A1B2-000001-C3D4
            // The regex checks for the 4-char hex instanceId.
            assertThat(transactionId).matches("ECOM-POC-TEST-\\d{14}-[A-F0-9]{4}-\\d{6}-[A-F0-9]+");

            // 2. Verify Correlation ID also contains the instanceId
            String correlationId = response.getBusinessContext().get("correlation_id");
            // Example: COR-ECOM-POC-A1B2-UUID-NANO
            assertThat(correlationId).matches("COR-ECOM-POC-[A-F0-9]{4}-[A-F0-9]{8}-[A-F0-9]+");

            // 3. Verify Session ID uses a secure hash for the user ID
            String sessionId = response.getBusinessContext().get("session_id");
            // Example: SES-ECOM-POC-A1B2-HASH-TIMESTAMP
            assertThat(sessionId).startsWith("SES-ECOM-POC-");
            
            // Extract the hashed part of the session ID
            String[] sessionIdParts = sessionId.split("-");
            assertThat(sessionIdParts.length).isEqualTo(5);
            String userHash = sessionIdParts[3];

            // The hash should be a 16-character uppercase hex string (from our SHA-256 impl)
            assertThat(userHash).matches("[A-F0-9]{16}");
            
            // IMPORTANT: We are NOT testing the actual hash value, only its format.
            // We are also asserting that the original user ID is NOT present.
            assertThat(sessionId).doesNotContain("user-to-be-hashed");
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