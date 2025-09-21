package com.example.poc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TracingIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private OpenTelemetry openTelemetry; // provided by TracingConfiguration

  @Autowired
  private InMemorySpanExporter exporter;

  @BeforeEach
    void setup() {
        exporter.reset();
    }

  @AfterEach
    void teardown() {
        exporter.reset();
    }

    @TestConfiguration
    static class OTelTestConfig {
        @Bean
        public InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
        @Bean
        public SpanProcessor inMemorySpanProcessor(InMemorySpanExporter exporter) {
            return SimpleSpanProcessor.create(exporter);
        }
    }

  @Test
  void completeFlow_emitsSpans_withBusinessAttributes() throws Exception {
    String json =
        "{\"operationId\":\"op-1\",\"data\":\"payload\",\"parameters\":\"k=v\",\"priority\":\"HIGH\",\"category\":\"BUSINESS\"}";

    mockMvc
        .perform(
            post("/api/v1/processing/complete-flow")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-ID", "integration-user")
                .header("X-Correlation-ID", "itest-corr")
                .content(json))
        .andExpect(status().isOk());

    // Read captured spans
    List<io.opentelemetry.sdk.trace.data.SpanData> spans = exporter.getFinishedSpanItems();
    assertThat(spans).isNotEmpty();

    boolean foundBusinessAttrs =
        spans.stream()
            .anyMatch(
                s ->
                    s.getAttributes()
                                .get(
                                    io.opentelemetry.api.common.AttributeKey.stringKey(
                                        "business.transaction.id"))
                            != null
                        && "integration-user"
                            .equals(
                                s.getAttributes()
                                    .get(
                                        io.opentelemetry.api.common.AttributeKey.stringKey(
                                            "user.id")))
                        && s.getAttributes()
                                .get(
                                    io.opentelemetry.api.common.AttributeKey.stringKey(
                                        "business.product.code"))
                            != null);

    assertThat(foundBusinessAttrs).as("at least one span contains business attributes").isTrue();
  }
}
