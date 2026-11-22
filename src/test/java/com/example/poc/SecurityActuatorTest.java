package com.example.poc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.poc.config.SecurityConfig;
import com.example.poc.service.BusinessContextIdGenerator;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Validates Actuator security constraints and production fail-fast rules.
 *
 * <p>Educational value (C3 & Security):
 * 1. Public probes: /actuator/health and /actuator/info are publicly accessible for orchestrator readiness.
 * 2. Protected endpoints: Sensitive metrics and management endpoints enforce HTTP Basic authentication.
 * 3. Fail-fast security: Starting in the 'prod' profile without setting distinct secrets throws
 *    an IllegalStateException at bootstrap time rather than silently deploying default credentials.
 */
@PocTest
@AutoConfigureMockMvc
class SecurityActuatorTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Security: /actuator/health is accessible without authentication")
  void healthEndpoint_isPubliclyAccessible() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Security: /actuator/metrics returns 401 Unauthorized when unauthenticated")
  void metricsEndpoint_requiresAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Security: /actuator/metrics returns 200 OK with valid HTTP Basic credentials")
  void metricsEndpoint_accessibleWithValidCredentials() throws Exception {
    String basicAuth = "Basic " + Base64.getEncoder().encodeToString("actuator:actuator".getBytes(StandardCharsets.UTF_8));
    mockMvc
        .perform(get("/actuator/metrics").header("Authorization", basicAuth))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("C3: Production profile fails fast if actuator password is blank or default")
  void prodProfile_failsFast_whenActuatorPasswordIsDefaultOrBlank() {
    SecurityConfig securityConfig = new SecurityConfig();
    MockEnvironment prodEnv = new MockEnvironment();
    prodEnv.setActiveProfiles("prod");

    // Default 'actuator' password in prod must fail
    assertThatThrownBy(() -> securityConfig.userDetailsService("actuator", "actuator", prodEnv))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Production security failure");

    // Blank password in prod must fail
    assertThatThrownBy(() -> securityConfig.userDetailsService("actuator", "", prodEnv))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Production security failure");
  }

  @Test
  @DisplayName("C7: Production profile fails fast if APP_SECURITY_SALT is default or blank")
  void prodProfile_failsFast_whenSaltIsDefaultOrBlank() {
    assertThatThrownBy(() -> new BusinessContextIdGenerator("ECOM-POC", "prod", "dev-only-salt"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_SECURITY_SALT must be explicitly set");

    assertThatThrownBy(() -> new BusinessContextIdGenerator("ECOM-POC", "prod", ""))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_SECURITY_SALT must be explicitly set");
  }
}
