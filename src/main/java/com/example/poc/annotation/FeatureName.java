package com.example.poc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * =================================================================================================
 * ARCHITECTURAL REVIEW
 * =================================================================================================
 *
 * <p>The `@FeatureName` annotation is used to associate an entire class with a specific business
 * feature.
 *
 * <p>Key Architectural Decisions & Best Practices: ------------------------------------------------
 * 1. `@Target(ElementType.TYPE)`: This is the correct scope. By applying it at the class/interface
 * level, it provides a broad context for all methods within that class. This is efficient, as you
 * don't need to repeat the feature name on every method. 2. `@Retention(RetentionPolicy.RUNTIME)`:
 * Correctly configured to ensure the annotation is available at runtime for AOP aspects to read. 3.
 * `String value()`: Follows the standard convention for single-attribute annotations, allowing for
 * concise usage (e.g., `@FeatureName("order-processing")`).
 *
 * <p>Role in the Architecture: ------------------------- - This annotation provides a higher level
 * of grouping than `@BusinessOperation` or `@ActionType`. It allows the tracing system to
 * understand that a set of different operations all belong to the same business feature. - For
 * example, a `UserManagementService` class could be annotated with
 * `@FeatureName("user-management")`. Then, all traces originating from this service (e.g.,
 * "create-user", "update-profile", "reset-password") would automatically be tagged as part of the
 * "user-management" feature. - This is extremely useful for creating dashboards and alerts that are
 * aligned with business domains or product teams.
 *
 * <p>Overall Feedback: ----------------- - The annotation is simple, clear, and serves a distinct
 * purpose in the hierarchy of business context annotations. - It complements the other annotations
 * well, providing a macro-level view that sits above the micro-level view of individual operations.
 * - It promotes a clean, organized code structure where related business logic is grouped into
 * classes that are explicitly tied to a business feature.
 *
 * <p>This is a valuable addition to the annotation suite, enabling powerful, feature-based
 * filtering and analysis of trace data.
 * =================================================================================================
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureName {
  /** The business feature name (e.g., "user-management", "order-processing") */
  String value();
}
