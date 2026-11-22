package com.example.poc;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.poc.controller.ProcessingController;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates that the '-parameters' compiler argument preserves method parameter names in bytecode.
 *
 * <p>Educational value (K7):
 * Spring AOP aspects and Spring MVC argument resolvers rely on bytecode parameter names to bind
 * request parameters, span attributes, and Spring Expression Language (SpEL) keys (#userId).
 * Without '-parameters' in JavaCompile options, parameter names default to arg0, arg1, causing
 * subtle SpEL and tracing failures in production.
 */
class CompilerParametersFlagTest {

  @Test
  @DisplayName("K7: Parameter names are preserved in bytecode via compiler '-parameters' flag")
  void parameterNames_arePreservedInBytecode() throws NoSuchMethodException {
    Method method =
        ProcessingController.class.getMethod(
            "testResiliencePattern", String.class, String.class, String.class);

    Parameter[] parameters = method.getParameters();
    assertThat(parameters).isNotEmpty();

    // Verify parameter name is present and matches the declared identifier rather than arg0
    assertThat(parameters[0].isNamePresent())
        .as("Compiler must retain parameter names in class bytecode via '-parameters'")
        .isTrue();

    assertThat(parameters[0].getName()).isEqualTo("pattern");
    assertThat(parameters[1].getName()).isEqualTo("userId");
    assertThat(parameters[2].getName()).isEqualTo("data");
  }
}
