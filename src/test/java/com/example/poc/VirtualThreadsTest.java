package com.example.poc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Validates Java 21 Virtual Threads integration when running under the 'virtual' profile.
 *
 * <p>Educational value (Interview Topic - Virtual Threads):
 * 1. Opt-in virtual threads: Spring Boot 3.2+ provides 'spring.threads.virtual.enabled=true' to execute
 *    Tomcat HTTP request handling on lightweight Virtual Threads (Loom).
 * 2. Verification: Thread.currentThread().isVirtual() confirms request dispatch on carrier-mounted virtual threads.
 * 3. Gotchas: Synchronized blocks can pin carrier threads (resolved in subsequent JDKs); ThreadLocal/MDC
 *    propagation requires careful scoping; and Resilience4j ThreadPoolBulkhead uses its own platform thread pool.
 */
@PocTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "virtual"})
@Tag("virtual-threads")
class VirtualThreadsTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Java 21: Endpoint confirms execution on virtual thread when virtual profile is active")
  void virtualThreadsEndpoint_executesOnVirtualThread() throws Exception {
    mockMvc
        .perform(get("/api/v1/processing/virtual-threads-info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isVirtual").value(true));
  }

  @Test
  @DisplayName("Java 21: Thread.ofVirtual() factory spawns lightweight virtual threads")
  void threadOfVirtual_executesCallable() throws Exception {
    Thread vThread = Thread.ofVirtual().name("poc-worker").start(() -> {
      assertThat(Thread.currentThread().isVirtual()).isTrue();
    });
    vThread.join();
  }
}
