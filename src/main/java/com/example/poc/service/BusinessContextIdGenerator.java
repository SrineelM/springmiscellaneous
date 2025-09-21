package com.example.poc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates custom business IDs following enterprise conventions discussed in the chat.
 * Implements the ID generation patterns mentioned in the distributed tracing strategy:
 * 
 * Format patterns:
 * - Business Transaction ID: {PRODUCT_CODE}-{ENVIRONMENT}-{TIMESTAMP}-{COUNTER}-{RANDOM}
 * - Correlation ID: COR-{PRODUCT_CODE}-{UUID_FRAGMENT}-{NANOTIME}
 * - Session ID: SES-{PRODUCT_CODE}-{USER_HASH}-{TIMESTAMP}
 * 
 * These IDs enable:
 * 1. Easy identification of request origin and flow
 * 2. Correlation across service boundaries
 * 3. User session tracking
 * 4. Performance analysis and debugging
 */
@Component
public class BusinessContextIdGenerator {
    
    private final String productCode;
    private final String environment;
    private final AtomicLong counter = new AtomicLong(0);
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ID_SEPARATOR = "-";
    
    public BusinessContextIdGenerator(
            @Value("${app.product.code:ECOM-POC}") String productCode,
            @Value("${spring.profiles.active:dev}") String environment) {
        this.productCode = productCode;
        this.environment = environment;
    }
    
    /**
     * Generates business transaction ID with hierarchical format for easy parsing.
     * Used for tracking complete business transactions across all service layers.
     * 
     * Example: ECOM-POC-DEV-20250921135400-001234-A7F3
     * 
     * Format breakdown:
     * - ECOM-POC: Product code for easy filtering in logs/traces
     * - DEV: Environment for deployment tracking
     * - 20250921135400: Timestamp for temporal ordering
     * - 001234: Incremental counter for uniqueness within same second
     * - A7F3: Random hex for additional entropy
     */
    public String generateBusinessTransactionId() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String counterStr = String.format("%06d", counter.incrementAndGet());
        String randomHex = Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000)).toUpperCase();
        
        return String.join(ID_SEPARATOR, 
            productCode, 
            environment.toUpperCase(), 
            timestamp, 
            counterStr, 
            randomHex);
    }
    
    /**
     * Generates correlation ID for request tracking across service boundaries.
     * Optimized for header transmission and log parsing.
     * 
     * Example: COR-ECOM-POC-A7B8C9D0-1A2B3C4D5E6F7890
     * 
     * Used in:
     * - HTTP headers for cross-service correlation
     * - Baggage propagation in OpenTelemetry
     * - Log correlation and error tracking
     */
    public String generateCorrelationId() {
        String uuidFragment = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String nanoTime = Long.toHexString(System.nanoTime()).toUpperCase();
        
        return String.join(ID_SEPARATOR, 
            "COR", 
            productCode, 
            uuidFragment, 
            nanoTime);
    }
    
    /**
     * Generates session-specific ID tied to user identity.
     * Enables user session tracking across distributed services.
     * 
     * Example: SES-ECOM-POC-A1B2C3D4-87654321
     * 
     * Format:
     * - SES: Session identifier prefix
     * - ECOM-POC: Product code
     * - A1B2C3D4: Hash of user ID for privacy
     * - 87654321: Timestamp suffix for session ordering
     */
    public String generateSessionId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            userId = "anonymous";
        }
        
        // Create deterministic hash of user ID for privacy while maintaining correlation
        String userHash = Integer.toHexString(Math.abs(userId.hashCode())).toUpperCase();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String timestampSuffix = timestamp.substring(Math.max(0, timestamp.length() - 8));
        
        return String.join(ID_SEPARATOR, 
            "SES", 
            productCode, 
            userHash, 
            timestampSuffix);
    }
    
    /**
     * Generates operation-specific ID for detailed operation tracking.
     * Used for tracking individual operations within a business transaction.
     * 
     * Example: OPR-PAYMENT-PROCESS-20250921-A7F3
     */
    public String generateOperationId(String operationType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomHex = Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000)).toUpperCase();
        
        return String.join(ID_SEPARATOR, 
            "OPR", 
            operationType.toUpperCase(), 
            timestamp, 
            randomHex);
    }
    
    /**
     * Generates trace-specific ID for OpenTelemetry integration.
     * Creates a business-friendly trace identifier that can be used in logs and monitoring.
     * 
     * Example: TRC-ECOM-POC-20250921135400-A7F3B2E1
     */
    public String generateTraceId() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String randomHex = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        
        return String.join(ID_SEPARATOR, 
            "TRC", 
            productCode, 
            timestamp, 
            randomHex);
    }
    
    /**
     * Generates batch ID for batch processing operations.
     * Useful for tracking bulk operations and data processing jobs.
     * 
     * Example: BCH-ECOM-POC-DAILY-20250921-001
     */
    public String generateBatchId(String batchType) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequence = String.format("%03d", counter.incrementAndGet() % 1000);
        
        return String.join(ID_SEPARATOR, 
            "BCH", 
            productCode, 
            batchType.toUpperCase(), 
            date, 
            sequence);
    }
    
    /**
     * Utility method to extract product code from any generated ID.
     * Useful for parsing and filtering operations.
     */
    public String extractProductCode(String generatedId) {
        if (generatedId == null || !generatedId.contains(ID_SEPARATOR)) {
            return null;
        }
        
        String[] parts = generatedId.split(ID_SEPARATOR);
        
        // For most ID types, product code is in position 1 or combined in positions 1-2
        if (parts.length >= 3 && ("COR".equals(parts[0]) || "SES".equals(parts[0]) || "TRC".equals(parts[0]))) {
            return parts[1]; // COR-ECOM-POC-... -> ECOM-POC is in position 1
        } else if (parts.length >= 2) {
            return parts[0]; // ECOM-POC-DEV-... -> ECOM-POC is in position 0
        }
        
        return null;
    }
    
    /**
     * Resets the counter (useful for testing)
     */
    public void resetCounter() {
        counter.set(0);
    }
    
    /**
     * Gets current counter value (useful for monitoring)
     */
    public long getCurrentCounter() {
        return counter.get();
    }
}