package com.example.poc.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Request model representing incoming processing requests.
 * Used to demonstrate distributed tracing across service boundaries
 * with comprehensive business context.
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