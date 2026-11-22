package com.example.poc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
 * Security configuration locking down Spring Boot Actuator endpoints.
 *
 * <p>Educational value:
 * 1. Health and info endpoints remain unauthenticated so container orchestrators (k8s liveness/readiness)
 *    can probe without credentials.
 * 2. Sensitive management endpoints (/actuator/**) require HTTP Basic authentication.
 * 3. In the 'prod' profile, actuator credentials must be explicitly configured via environment variables
 *    (ACTUATOR_USER and ACTUATOR_PASSWORD), failing fast at startup if missing or left as default.
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
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .httpBasic(Customizer.withDefaults());
    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService(
      @Value("${app.actuator.username:actuator}") String user,
      @Value("${app.actuator.password:actuator}") String password,
      Environment env) {
    boolean isProd = env.matchesProfiles("prod");
    if (isProd) {
      if (password == null || password.isBlank() || "actuator".equals(password)) {
        throw new IllegalStateException(
            "Production security failure: ACTUATOR_PASSWORD must be explicitly configured and cannot use default credentials.");
      }
      if (user == null || user.isBlank()) {
        throw new IllegalStateException(
            "Production security failure: ACTUATOR_USER must be explicitly configured.");
      }
    }

    PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    UserDetails actuatorUser =
        User.withUsername(user)
            .password(encoder.encode(password))
            .roles("ACTUATOR")
            .build();
    return new InMemoryUserDetailsManager(actuatorUser);
  }
}
