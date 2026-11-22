package com.example.poc.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Immutable request record for the processing flow.
 *
 * <p>Educational value:
 * 1. Java 21 Record: Eliminates mutable DTO state, ensuring request parameters cannot be modified in-flight.
 * 2. Overloaded Constructors: Allows concise creation with sensible defaults for priority ("NORMAL") and category ("GENERAL").
 * 3. Jackson Compatibility: Deserializes directly from JSON into record components.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessingRequest(
    String operationId,
    String data,
    String parameters,
    String priority,
    String category) {

  public ProcessingRequest(String operationId, String data, String parameters) {
    this(operationId, data, parameters, "NORMAL", "GENERAL");
  }

  public String getOperationId() {
    return operationId();
  }

  public String getData() {
    return data();
  }

  public String getParameters() {
    return parameters();
  }

  public String getPriority() {
    return priority();
  }

  public String getCategory() {
    return category();
  }
}
