package com.example.poc;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.poc.model.MtrCustomer;
import com.example.poc.repository.MtrCustomerRepository;
import com.example.poc.service.MtrCustomerService;
import org.junit.jupiter.api.Test;

class MtrCustomerServiceTest {

  @Test
  void returnsHardcodedCustomer() {
    // Arrange: mock repo with simple lambda
    MtrCustomerRepository repo = new MtrCustomerRepository();
    MtrCustomerService service = new MtrCustomerService(repo);

    // Act
    MtrCustomer c = service.getCustomer("123");

    // Assert
    assertThat(c).isNotNull();
    assertThat(c.getId()).isEqualTo("123");
    assertThat(c.getName()).isEqualTo("Jane Doe");
    assertThat(c.getTier()).isEqualTo("GOLD");
  }
}
