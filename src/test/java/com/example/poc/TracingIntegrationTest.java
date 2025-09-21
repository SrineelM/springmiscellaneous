package com.example.poc;

import com.example.poc.model.ProcessingRequest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TracingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpenTelemetry openTelemetry; // provided by TracingConfiguration

    private InMemorySpanExporter exporter;

    @BeforeEach
    void setup() {
        // Wire an additional SimpleSpanProcessor to the existing SDK to capture exported spans in-memory
        exporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        // Register a temporary SDK for the test scope to capture spans
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal();
    }

    @AfterEach
    void teardown() {
        exporter.reset();
    }

    @Test
    void completeFlow_emitsSpans_withBusinessAttributes() throws Exception {
        String json = "{\"operationId\":\"op-1\",\"data\":\"payload\",\"parameters\":\"k=v\",\"priority\":\"HIGH\",\"category\":\"BUSINESS\"}";

        mockMvc.perform(post("/api/v1/processing/complete-flow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-ID", "integration-user")
                        .header("X-Correlation-ID", "itest-corr")
                        .content(json))
                .andExpect(status().isOk());

        // Read captured spans
        List<io.opentelemetry.sdk.trace.data.SpanData> spans = exporter.getFinishedSpanItems();
        assertThat(spans).isNotEmpty();

        boolean foundBusinessAttrs = spans.stream().anyMatch(s ->
                s.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("business.transaction.id")) != null
                        && "integration-user".equals(
                        s.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("user.id")))
                        && s.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("business.product.code")) != null
        );

        assertThat(foundBusinessAttrs).as("at least one span contains business attributes").isTrue();
    }
}
