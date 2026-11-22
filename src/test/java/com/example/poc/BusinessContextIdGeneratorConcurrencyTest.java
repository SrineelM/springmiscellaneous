package com.example.poc;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.poc.service.BusinessContextIdGenerator;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates thread safety and unique identifier generation under heavy concurrent contention.
 *
 * <p>Educational value (K6):
 * Under high concurrency across multiple threads, ID generators relying on unsynchronized counters,
 * shared SimpleDateFormats, or static MessageDigests suffer from race conditions and collisions.
 * This test spins up 50 threads generating 100 IDs each to verify absolute uniqueness.
 */
class BusinessContextIdGeneratorConcurrencyTest {

  @Test
  @DisplayName("K6: Uniqueness of generated business transaction IDs under 50 concurrent threads")
  void businessTransactionId_isUniqueUnderHighConcurrency() throws InterruptedException {
    int threadCount = 50;
    int iterationsPerThread = 100;
    int totalExpected = threadCount * iterationsPerThread;

    BusinessContextIdGenerator generator =
        new BusinessContextIdGenerator("ECOM-POC", "dev", "test-salt");

    Set<String> generatedIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(threadCount);

    for (int t = 0; t < threadCount; t++) {
      executor.submit(() -> {
        try {
          startLatch.await();
          for (int i = 0; i < iterationsPerThread; i++) {
            generatedIds.add(generator.generateBusinessTransactionId());
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          finishLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(completed).isTrue();
    assertThat(generatedIds).hasSize(totalExpected);
  }
}
