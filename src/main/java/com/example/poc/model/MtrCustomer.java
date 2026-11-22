package com.example.poc.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Immutable domain record representing a Customer in the Micrometer Tracing demo.
 *
 * <p>Educational value:
 * 1. Java 21 Record: Replaces verbose DTO boilerplate with an immutable, transparent carrier for data.
 * 2. Jackson Serialization: Modern Jackson versions automatically introspect record components.
 * 3. Backward Compatibility: Provides accessor bridges (getId(), getName(), getTier()) and a fluent
 *    builder to preserve complete source compatibility with existing code.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MtrCustomer(String id, String name, String tier) {

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id();
  }

  public String getName() {
    return name();
  }

  public String getTier() {
    return tier();
  }

  public static class Builder {
    private String id;
    private String name;
    private String tier;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder tier(String tier) {
      this.tier = tier;
      return this;
    }

    public MtrCustomer build() {
      return new MtrCustomer(id, name, tier);
    }
  }
}
