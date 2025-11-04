package com.example.poc.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Simple DTO for the Micrometer demo. Kept intentionally minimal and Jackson-friendly (default ctor
 * + getters/setters).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MtrCustomer {
  private String id;
  private String name;
  private String tier;

  /** Default constructor required by Jackson. */
  public MtrCustomer() {}

  public MtrCustomer(String id, String name, String tier) {
    this.id = id;
    this.name = name;
    this.tier = tier;
  }

  /** Convenience builder for terse construction in examples/tests. */
  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getTier() {
    return tier;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setTier(String tier) {
    this.tier = tier;
  }

  @Override
  public String toString() {
    return "MtrCustomer{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", tier='"
        + tier
        + '\''
        + '}';
  }

  /** Fluent builder for {@link MtrCustomer}. */
  public static class Builder {
    private final MtrCustomer c = new MtrCustomer();

    public Builder id(String id) {
      c.setId(id);
      return this;
    }

    public Builder name(String name) {
      c.setName(name);
      return this;
    }

    public Builder tier(String tier) {
      c.setTier(tier);
      return this;
    }

    public MtrCustomer build() {
      return c;
    }
  }
}
