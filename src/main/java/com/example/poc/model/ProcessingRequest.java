package com.example.poc.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * =================================================================================================
 * ARCHITECTURAL REVIEW
 * =================================================================================================
 * 
 * The `ProcessingRequest` class is a Data Transfer Object (DTO) used to model the payload of
 * incoming POST requests to the `ProcessingController`.
 * 
 * Key Architectural Decisions & Best Practices:
 * ------------------------------------------------
 * 1.  POJO (Plain Old Java Object): The class is a simple POJO with private fields, a no-argument
 *     constructor, and public getters/setters. This is the standard and correct way to create DTOs
 *     that can be easily serialized and deserialized by frameworks like Jackson (which Spring Boot
 *     uses by default).
 * 2.  No-Argument Constructor: The presence of a `public ProcessingRequest() {}` constructor is
 *     essential for JSON deserialization frameworks. They need to be able to instantiate the object
 *     before they can populate its fields.
 * 3.  Multiple Constructors: Providing additional constructors is a good practice for making the
 *     object easier to create in test code or other parts of the application.
 * 4.  Immutability (or lack thereof): This class is mutable (it has setters). For a simple request
 *     DTO, this is perfectly acceptable. In more complex, state-sensitive scenarios (especially in
 *     concurrent environments), an immutable DTO (with final fields and a builder pattern) might be
 *     preferred, but for this use case, a mutable POJO is fine.
 * 5.  `toString()` Method: A well-implemented `toString()` method is provided, which is invaluable
 *     for logging and debugging. It gives a clear, human-readable representation of the object's state.
 * 
 * Role in the Architecture:
 * -------------------------
 * - It defines the contract for the `/api/v1/processing/complete-flow` endpoint's request body.
 * - It acts as a container to transport data from the client (e.g., a web browser or another service)
 *   into the application in a structured way.
 * - The fields in this class (`operationId`, `priority`, `category`) are themselves sources of
 *   business context that can be used to enrich traces.
 * 
 * Overall Feedback:
 * -----------------
 * - The class is a standard, well-implemented DTO that perfectly serves its purpose.
 * - It's simple, clean, and follows all the necessary conventions for working with Spring Web and
 *   JSON serialization.
 * - There are no major weaknesses. One minor suggestion for a production system could be to add
 *   JSR 303 validation annotations (e.g., `@NotNull`, `@Size`) to the fields to enforce data
 *   integrity at the controller boundary, but for a POC, this is not necessary.
 * 
 * This is a solid, textbook example of a request DTO.
 * =================================================================================================
 */
public class ProcessingRequest {
    
    private String operationId;
    private String data;
    private String parameters;
    private String priority;
    private String category;
    
    public ProcessingRequest() {}
    
    public ProcessingRequest(String operationId, String data, String parameters) {
        this.operationId = operationId;
        this.data = data;
        this.parameters = parameters;
        this.priority = "NORMAL";
        this.category = "GENERAL";
    }
    
    public ProcessingRequest(String operationId, String data, String parameters, String priority, String category) {
        this.operationId = operationId;
        this.data = data;
        this.parameters = parameters;
        this.priority = priority;
        this.category = category;
    }
    
    // Getters and Setters
    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
    
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    @Override
    public String toString() {
        return String.format("ProcessingRequest{operationId='%s', data='%s', parameters='%s', priority='%s', category='%s'}", 
                           operationId, data, parameters, priority, category);
    }
}