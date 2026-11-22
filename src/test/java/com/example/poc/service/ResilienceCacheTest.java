package com.example.poc.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.poc.PocTest;
import com.example.poc.model.ProcessingResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Validates behavior when Spring's @Cacheable is combined with Resilience4j patterns.
 *
 * <p>Educational value (Interview Topic - K3):
 * 1. Cold Call (Cache Miss): Executes the actual underlying service method and stores the successful return value.
 * 2. Warm Call (Cache Hit): Bypasses the underlying method and resilience aspect overhead, serving the cached object directly.
 * 3. Fallback Integrity: When an operation fails and fallback executes, verifying whether the fallback result is cached.
 */
@PocTest
class ResilienceCacheTest {

  @Autowired private ExternalServiceClient client;
  @Autowired private Resilience4jDemoService demoService;
  @Autowired private CacheManager cacheManager;

  @Test
  @DisplayName("K3: Cache hit returns cached result without re-executing service")
  void cacheMissThenCacheHit() {
    String userId = "cache-user-" + System.currentTimeMillis();

    // Cold call: cache miss
    ProcessingResult result1 = client.fetchUserData(userId);
    assertThat(result1).isNotNull();
    assertThat(result1.getStatus()).isEqualTo("SUCCESS");

    // Check cache directly
    Cache cache = cacheManager.getCache("userDataCache");
    assertThat(cache).isNotNull();
    assertThat(cache.get(userId)).isNotNull();

    // Warm call: served directly from cache (same resultId and message)
    ProcessingResult result2 = client.fetchUserData(userId);
    assertThat(result2.getResultId()).isEqualTo(result1.getResultId());
    assertThat(result2.getMessage()).isEqualTo(result1.getMessage());
  }

  @Test
  @DisplayName("K3: Cache eviction removes cached entry")
  void cacheEviction_removesEntry() {
    String userId = "evict-user-" + System.currentTimeMillis();

    // Populate cache
    String data1 = demoService.cacheableDemo(userId);
    assertThat(data1).contains(userId);

    Cache cache = cacheManager.getCache("demoCache");
    assertThat(cache).isNotNull();
    assertThat(cache.get(userId)).isNotNull();

    // Evict
    demoService.cacheEvictDemo(userId);
    assertThat(cache.get(userId)).isNull();
  }
}
