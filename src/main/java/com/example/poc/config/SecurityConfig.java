package com.example.poc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration to restrict Actuator endpoints only. - Application APIs are permitted
 * without authentication. - /actuator/health and /actuator/info are public (common practice). - All
 * other /actuator/** require HTTP Basic authentication.
 *
 * <p>For production, plug in your organization’s user store (e.g., LDAP/OIDC) and disable the
 * in-memory user.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .authenticated() // NOTE: lock down actuator only
                    .anyRequest()
                    .permitAll())
        .httpBasic(Customizer.withDefaults());
    // TODO(prod): Move actuator to a dedicated management port and restrict by network policy/IP
    // allowlist
    // TODO(auth): Replace basic auth with OIDC/LDAP and role-based access controls for actuator
    // endpoints
    return http.build();
  }

  /**
   * In-memory user for local development/testing. Use environment variables for username/password
   * or externalize via Vault/Config in real deployments.
   */
  @Bean
  public UserDetailsService userDetailsService(
      @Value("${spring.security.user:actuator}") String user,
      @Value("${spring.security.password:actuator}") String password) {
    PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    UserDetails actuatorUser =
        User.withUsername(user)
            .password(encoder.encode(password))
            .roles("ACTUATOR") // NOTE: dev-only in-memory user; replace with OIDC/LDAP in prod
            .build();
    return new InMemoryUserDetailsManager(actuatorUser);
  }
}
