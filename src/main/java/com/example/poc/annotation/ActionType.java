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
 * This annotation, `@ActionType`, is designed to add business-level context to a method,
 * specifically categorizing the type of action being performed.
 * 
 * Key Architectural Decisions & Best Practices:
 * ------------------------------------------------
 * 1.  `@Target(ElementType.METHOD)`: This is correctly scoped. The annotation is intended to describe
 *     the action of a specific method, so applying it at the method level is the right choice.
 * 2.  `@Retention(RetentionPolicy.RUNTIME)`: This is crucial. For the annotation to be introspected
 *     at runtime by the AOP aspect (`DistributedTracingAspect`), it must be retained in the compiled
 *     class file and be available via reflection. This is correctly configured.
 * 3.  `String value()`: Using `value()` as the attribute name is a convention in Java that allows for
 *     a shorthand syntax (e.g., `@ActionType("CREATE")` instead of `@ActionType(value = "CREATE")`).
 *     This improves readability and is a good practice for single-attribute annotations.
 * 
 * Role in the Architecture:
 * -------------------------
 * - This annotation plays a key role in enriching the distributed traces with business-relevant
 *   information. Instead of just knowing that a method was called, the tracing system can now
 *   categorize the call as a specific business action (e.g., "COMPLETE_PROCESSING").
 * - This data can be invaluable for business analytics, monitoring, and filtering traces. For example,
 *   a team could easily find all traces related to "USER_LOGIN" actions.
 * 
 * Overall Feedback:
 * -----------------
 * - The annotation is simple, well-defined, and follows Java best practices.
 * - It serves a clear and important purpose in the overall distributed tracing strategy by bridging
 *   the gap between technical execution and business logic.
 * - It is a good example of creating a "Domain-Specific Language" (DSL) for describing business
 *   operations within the code, making the code more self-documenting.
 * 
 * This is a well-designed and effective annotation for its intended purpose.
 * =================================================================================================
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActionType {
    /**
     * The action type (e.g., "CREATE", "READ", "UPDATE", "DELETE", "PROCESS")
     */
    String value();
}