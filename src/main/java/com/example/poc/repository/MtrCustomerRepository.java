package com.example.poc.repository;

import com.example.poc.model.MtrCustomer;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Repository;

/**
 * Mock repository for the Micrometer demo. Serves a hard-coded customer and emits an observation
 * (bridged to an OTel span) via method-level @Observed. Keep names/tags low-cardinality for stable
 * metrics.
 */
@Repository
public class MtrCustomerRepository {

  /** Finds a mock customer by ID; simulates small latency. */
  @Observed(
      name = "mtr.customer.repository",
      lowCardinalityKeyValues = {"customer.id", "{id}"})
  public MtrCustomer findById(String id) {
    // Simulate small DB latency
    try {
      Thread.sleep(50);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
    // Return hardcoded customer (acts like a DB result)
    return MtrCustomer.builder().id(id).name("Jane Doe").tier("GOLD").build();
  }
}
