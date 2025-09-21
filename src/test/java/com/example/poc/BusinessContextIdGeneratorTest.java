package com.example.poc;

import static org.junit.jupiter.api.Assertions.*;

import com.example.poc.service.BusinessContextIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BusinessContextIdGeneratorTest {

  private BusinessContextIdGenerator generator;

  @BeforeEach
  void setup() {
    generator = new BusinessContextIdGenerator("ECOM-POC", "dev", "test-salt");
    generator.resetCounter();
  }

  @Test
  void generatesBusinessTransactionId_withExpectedParts() {
    String id = generator.generateBusinessTransactionId();
    assertNotNull(id);
    String[] parts = id.split("-");
    // product, env, ts, instance, counter, rand
    assertTrue(parts.length >= 6, "Unexpected txn id format: " + id);
    assertEquals("ECOM_POC", parts[0]);
    assertEquals("DEV", parts[1]);
    assertEquals(generator.getInstanceId(), parts[3]);
  }

  @Test
  void generatesCorrelationId_withPrefixAndProduct() {
    String id = generator.generateCorrelationId();
    assertTrue(id.startsWith("COR-"));
    String[] parts = id.split("-");
    assertEquals("ECOM_POC", parts[1]);
  }

  @Test
  void sessionId_usesSaltedHashAndInstance() {
    String id = generator.generateSessionId("user-123");
    String[] parts = id.split("-");
    assertEquals("SES", parts[0]);
    assertEquals("ECOM_POC", parts[1]);
    assertEquals(generator.getInstanceId(), parts[2]);
    assertTrue(parts[3].matches("[0-9A-F]{16}"), "Expected 16-hex salted hash: " + parts[3]);
  }

  @Test
  void extractProductCode_isPrefixAware() {
    String txn = generator.generateBusinessTransactionId();
    String cor = generator.generateCorrelationId();
    String ses = generator.generateSessionId("u");
    String trc = generator.generateTraceId();
    assertEquals("ECOM_POC", generator.extractProductCode(txn));
    assertEquals("ECOM_POC", generator.extractProductCode(cor));
    assertEquals("ECOM_POC", generator.extractProductCode(ses));
    assertEquals("ECOM_POC", generator.extractProductCode(trc));
  }
}
