package com.example.poc.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates JSON serialization and deserialization for modernized Java 21 Records.
 *
 * <p>Educational value:
 * Ensures Jackson seamlessly serializes and deserializes record components, validating that
 * converting legacy POJOs to records maintains 100% wire-format compatibility.
 */
class RecordsJacksonTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Java 21: MtrCustomer record serializes and deserializes correctly")
  void mtrCustomerRecord_jsonRoundTrip() throws Exception {
    MtrCustomer customer = new MtrCustomer("C-123", "Jane Doe", "GOLD");

    String json = objectMapper.writeValueAsString(customer);
    assertThat(json).contains("\"id\":\"C-123\"");
    assertThat(json).contains("\"name\":\"Jane Doe\"");
    assertThat(json).contains("\"tier\":\"GOLD\"");

    MtrCustomer deserialized = objectMapper.readValue(json, MtrCustomer.class);
    assertThat(deserialized).isEqualTo(customer);
    assertThat(deserialized.id()).isEqualTo("C-123");
    assertThat(deserialized.getId()).isEqualTo("C-123");
    assertThat(deserialized.getName()).isEqualTo("Jane Doe");
  }

  @Test
  @DisplayName("Java 21: ProcessingRequest record serializes and deserializes correctly")
  void processingRequestRecord_jsonRoundTrip() throws Exception {
    String inputJson =
        "{\"operationId\":\"OP-999\",\"data\":\"payload\",\"parameters\":\"k=v\",\"priority\":\"HIGH\",\"category\":\"PAYMENT\"}";

    ProcessingRequest request = objectMapper.readValue(inputJson, ProcessingRequest.class);
    assertThat(request.operationId()).isEqualTo("OP-999");
    assertThat(request.getOperationId()).isEqualTo("OP-999");
    assertThat(request.data()).isEqualTo("payload");
    assertThat(request.priority()).isEqualTo("HIGH");
    assertThat(request.category()).isEqualTo("PAYMENT");

    String outputJson = objectMapper.writeValueAsString(request);
    assertThat(outputJson).contains("\"operationId\":\"OP-999\"");
  }
}
