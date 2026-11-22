package com.example.poc.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * RestTemplate interceptor that automatically creates CLIENT spans and propagates trace context via
 * HTTP headers for outgoing HTTP requests.
 *
 * <p>This interceptor: - Creates a CLIENT span for each HTTP request - Adds HTTP semantic
 * attributes (method, URL, status code, etc.) - Propagates W3C trace context headers - Records
 * exceptions and sets appropriate span status
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @Bean
 * public RestTemplate restTemplate(TracingRestTemplateInterceptor interceptor) {
 *     RestTemplate template = new RestTemplate();
 *     template.getInterceptors().add(interceptor);
 *     return template;
 * }
 * }</pre>
 */
public class TracingRestTemplateInterceptor implements ClientHttpRequestInterceptor {
  private static final Logger logger =
      LoggerFactory.getLogger(TracingRestTemplateInterceptor.class);

  private final Tracer tracer;
  private final OpenTelemetry openTelemetry;

  public TracingRestTemplateInterceptor(Tracer tracer, OpenTelemetry openTelemetry) {
    this.tracer = tracer;
    this.openTelemetry = openTelemetry;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    URI uri = request.getURI();
    String spanName = buildSpanName(request.getMethod().name(), uri);

    // Create CLIENT span for outgoing HTTP request
    Span span =
        tracer
            .spanBuilder(spanName)
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("http.method", request.getMethod().name())
            .setAttribute("http.url", uri.toString())
            .setAttribute("http.scheme", uri.getScheme())
            .setAttribute("http.host", uri.getHost())
            .setAttribute("http.target", uri.getPath())
            .setAttribute("component", "rest-template")
            .startSpan();

    if (uri.getPort() != -1) {
      span.setAttribute("http.port", uri.getPort());
    }

    if (uri.getQuery() != null) {
      span.setAttribute("http.query", uri.getQuery());
    }

    if (body != null && body.length > 0) {
      span.setAttribute("http.request_content_length", body.length);
    }

    try (Scope scope = span.makeCurrent()) {
      // Propagate trace context via HTTP headers
      openTelemetry
          .getPropagators()
          .getTextMapPropagator()
          .inject(Context.current(), request, HTTP_HEADER_SETTER);

      span.addEvent("http.request.sent");

      ClientHttpResponse response = execution.execute(request, body);

      // Add response attributes
      int statusCode = response.getStatusCode().value();
      span.setAttribute("http.status_code", statusCode);
      span.setAttribute("http.response_content_length", response.getHeaders().getContentLength());

      // Set span status based on HTTP status code
      if (statusCode >= 400) {
        span.setStatus(StatusCode.ERROR);
        if (statusCode >= 500) {
          span.setAttribute("error.type", "server_error");
        } else {
          span.setAttribute("error.type", "client_error");
        }
      } else {
        span.setStatus(StatusCode.OK);
      }

      span.addEvent("http.response.received");

      logger.debug("HTTP request traced: {} {} - Status: {}", request.getMethod(), uri, statusCode);

      return response;

    } catch (IOException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, "HTTP request failed: " + e.getMessage());
      span.setAttribute("error.type", "io_exception");
      logger.error("HTTP request failed: {} {}", request.getMethod(), uri, e);
      throw e;
    } finally {
      span.end();
    }
  }

  /**
   * Builds a low-cardinality span name for HTTP requests.
   *
   * @param method HTTP method
   * @param uri Request URI
   * @return Span name in format "HTTP {METHOD} {path}"
   */
  private String buildSpanName(String method, URI uri) {
    String path = uri.getPath();
    if (path == null || path.isEmpty()) {
      path = "/";
    }
    // Use the path without query parameters for low cardinality
    return "HTTP " + method + " " + path;
  }

  /** TextMapSetter for injecting trace context into HTTP request headers. */
  private static final TextMapSetter<HttpRequest> HTTP_HEADER_SETTER =
      (carrier, key, value) -> {
        if (carrier != null && key != null && value != null) {
          carrier.getHeaders().set(key, value);
        }
      };
}
