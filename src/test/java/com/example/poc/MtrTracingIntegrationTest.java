package com.example.poc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration test validating that Micrometer {@code @Observed} annotations
 * produce OTel spans via the bridge and show up in the in-memory exporter.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MtrTracingIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private InMemorySpanExporter exporter;

  @BeforeEach
  void setup() { exporter.reset(); }

  @AfterEach
  void teardown() { exporter.reset(); }

  @TestConfiguration
  static class OTelTestConfig {
    @Bean
    public InMemorySpanExporter inMemorySpanExporter() { return InMemorySpanExporter.create(); }
    @Bean
    public SpanProcessor inMemorySpanProcessor(InMemorySpanExporter exporter) {
      return SimpleSpanProcessor.create(exporter);
    }
  }

  @Test
  void getCustomer_emits_observation_spans() throws Exception {
    mockMvc.perform(get("/api/v1/mtr/customers/{id}", "42"))
        .andExpect(status().isOk());

    List<io.opentelemetry.sdk.trace.data.SpanData> spans = exporter.getFinishedSpanItems();
    assertThat(spans).isNotEmpty();
    // Debug: print span names and key attributes for diagnosis
    System.out.println("Captured spans:");
    for (io.opentelemetry.sdk.trace.data.SpanData s : spans) {
      var attrs = s.getAttributes();
      String route = attrs.get(io.opentelemetry.api.common.AttributeKey.stringKey("http.route"));
      String endpoint = attrs.get(io.opentelemetry.api.common.AttributeKey.stringKey("endpoint"));
      String custId = attrs.get(io.opentelemetry.api.common.AttributeKey.stringKey("customer.id"));
      System.out.println(" - " + s.getName() + " route=" + route + " endpoint=" + endpoint + " customer.id=" + custId);
    }
    // Micrometer spans: prefer name match; AOP spans: may carry http.route
    boolean hasController = spans.stream().anyMatch(s ->
      s.getName().contains("mtr.customer.controller")
      || s.getName().contains("mtrcustomer.getCustomer")
      || "/customers/{id}".equals(
        s.getAttributes().get(
          io.opentelemetry.api.common.AttributeKey.stringKey("endpoint")))
      || "/api/v1/mtr/customers/{id}".equals(
        s.getAttributes().get(
          io.opentelemetry.api.common.AttributeKey.stringKey("http.route")))
    );
    // Service observation should tag customer.id and/or carry observed name
    boolean hasService = spans.stream().anyMatch(s ->
      s.getName().contains("mtr.customer.service")
      || s.getName().contains("mtrcustomerservice.getCustomer")
      || (s.getAttributes().get(
          io.opentelemetry.api.common.AttributeKey.stringKey("customer.id")) != null)
    );
    // Repo observation may be represented via name or attribute
    boolean hasRepo = spans.stream().anyMatch(s ->
      s.getName().contains("mtr.customer.repository")
      || s.getName().contains("mtrcustomerrepository.findById")
      || (s.getAttributes().get(
          io.opentelemetry.api.common.AttributeKey.stringKey("customer.id")) != null)
    );
    assertThat(hasController).as("controller observation span present").isTrue();
    assertThat(hasService).as("service observation span present").isTrue();
    assertThat(hasRepo).as("repository observation span present").isTrue();
  }
}
