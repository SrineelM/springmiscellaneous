package com.example.poc.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Result model representing processing outcomes.
 * Contains comprehensive business context and technical metadata
 * for distributed tracing correlation and monitoring.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessingResult {
    
    private String resultId;
    private String message;
    private String status;
    private String sourceService;
    private String operationType;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private Long processingTimeMs;
    private Map<String, Object> metadata;
    private Map<String, String> businessContext;
    
    public ProcessingResult() {
        this.timestamp = LocalDateTime.now();
        this.status = "SUCCESS";
        this.metadata = new HashMap<>();
        this.businessContext = new HashMap<>();
    }
    
    public ProcessingResult(String resultId, String message) {
        this();
        this.resultId = resultId;
        this.message = message;
    }
    
    public ProcessingResult(String resultId, String message, LocalDateTime timestamp) {
        this(resultId, message);
        this.timestamp = timestamp;
    }
    
    public ProcessingResult(String resultId, String message, String status, String sourceService) {
        this(resultId, message);
        this.status = status;
        this.sourceService = sourceService;
    }
    
    // Builder pattern for complex construction
    public static ProcessingResultBuilder builder() {
        return new ProcessingResultBuilder();
    }
    
    // Utility methods for business context
    public ProcessingResult addBusinessContext(String key, String value) {
        if (this.businessContext == null) {
            this.businessContext = new HashMap<>();
        }
        this.businessContext.put(key, value);
        return this;
    }
    
    public ProcessingResult addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
    
    // Getters and Setters
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }
    
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Map<String, String> getBusinessContext() { return businessContext; }
    public void setBusinessContext(Map<String, String> businessContext) { this.businessContext = businessContext; }
    
    @Override
    public String toString() {
        return String.format("ProcessingResult{resultId='%s', message='%s', status='%s', sourceService='%s', timestamp=%s}", 
                           resultId, message, status, sourceService, timestamp);
    }
    
    /**
     * Builder class for ProcessingResult
     */
    public static class ProcessingResultBuilder {
        private ProcessingResult result;
        
        public ProcessingResultBuilder() {
            this.result = new ProcessingResult();
        }
        
        public ProcessingResultBuilder resultId(String resultId) {
            result.setResultId(resultId);
            return this;
        }
        
        public ProcessingResultBuilder message(String message) {
            result.setMessage(message);
            return this;
        }
        
        public ProcessingResultBuilder status(String status) {
            result.setStatus(status);
            return this;
        }
        
        public ProcessingResultBuilder sourceService(String sourceService) {
            result.setSourceService(sourceService);
            return this;
        }
        
        public ProcessingResultBuilder operationType(String operationType) {
            result.setOperationType(operationType);
            return this;
        }
        
        public ProcessingResultBuilder processingTimeMs(Long processingTimeMs) {
            result.setProcessingTimeMs(processingTimeMs);
            return this;
        }
        
        public ProcessingResultBuilder addBusinessContext(String key, String value) {
            result.addBusinessContext(key, value);
            return this;
        }
        
        public ProcessingResultBuilder addMetadata(String key, Object value) {
            result.addMetadata(key, value);
            return this;
        }
        
        public ProcessingResult build() {
            return result;
        }
    }
}