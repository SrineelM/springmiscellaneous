package com.example.poc.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generates structured business IDs (txn/correlation/session/trace/batch) for
 * use in spans, baggage and logs. IDs are human-readable, include an instance
 * token for scale, and use salted hashing for privacy where needed.
 */
@Component
public class BusinessContextIdGenerator {
  private static final Logger logger = LoggerFactory.getLogger(BusinessContextIdGenerator.class);

  private final String productCode;
  // Normalized token for use inside hyphen-delimited IDs to avoid parsing ambiguity
  private final String productToken;
  private final String environment;
  private final String instanceId; // Unique ID for this application instance
  private final String salt; // Salt for secure hashing
  private final AtomicLong counter = new AtomicLong(0);

  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final String ID_SEPARATOR = "-";

  public BusinessContextIdGenerator(
      @Value("${app.product.code:ECOM-POC}") String productCode,
      @Value("${spring.profiles.active:dev}") String environment,
      @Value("${app.security.salt:default-salt-for-poc}") String salt) {
    this.productCode = productCode;
    // Replace non-alphanumeric characters to prevent '-' from breaking tokenization
    this.productToken = productCode.replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
    this.environment = environment;
    this.salt = salt;
    // NOTE (scalability): Generate a short, random per-instance ID to prevent collisions under
    // horizontal scale.
    // In k8s, you could include node/zone hints via env for even better uniqueness if desired.
    this.instanceId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
  }

  /** Business transaction ID: PRODUCT-ENV-TIMESTAMP-INSTANCE-COUNTER-RAND. */
  public String generateBusinessTransactionId() {
    String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
    String counterStr = String.format("%06d", counter.incrementAndGet());
    String randomHex =
        Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000)).toUpperCase();

    return String.join(
        ID_SEPARATOR,
        productToken,
        environment.toUpperCase(),
        timestamp,
        instanceId,
        counterStr,
        randomHex);
  }

  /** Correlation ID for cross-service tracking: COR-PRODUCT-INSTANCE-UUID8-NANOTIMEHEX. */
  public String generateCorrelationId() {
    String uuidFragment = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    String nanoTime = Long.toHexString(System.nanoTime()).toUpperCase();

    return String.join(ID_SEPARATOR, "COR", productToken, instanceId, uuidFragment, nanoTime);
  }

  /** Session ID tied to user (salted hash), includes instance id and ts suffix. */
  public String generateSessionId(String userId) {
    if (userId == null || userId.trim().isEmpty()) {
      userId = "anonymous";
    }

    // Create a secure, salted hash of the user ID for privacy
    String userHash = secureHash(userId, salt);
    String timestamp = String.valueOf(System.currentTimeMillis());
    String timestampSuffix = timestamp.substring(Math.max(0, timestamp.length() - 8));

    return String.join(ID_SEPARATOR, "SES", productToken, instanceId, userHash, timestampSuffix);
  }

  /** Operation ID for fine-grained tracking: OPR-TYPE-INSTANCE-DATE-RAND. */
  public String generateOperationId(String operationType) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String randomHex =
        Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000)).toUpperCase();

    return String.join(
        ID_SEPARATOR, "OPR", operationType.toUpperCase(), instanceId, timestamp, randomHex);
  }

  /** Business-friendly trace identifier: TRC-PRODUCT-INSTANCE-TIMESTAMP-RAND8. */
  public String generateTraceId() {
    String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
    String randomHex = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

    return String.join(ID_SEPARATOR, "TRC", productToken, instanceId, timestamp, randomHex);
  }

  /** Batch ID: BCH-PRODUCT-BATCHTYPE-INSTANCE-DATE-SEQ. */
  public String generateBatchId(String batchType) {
    String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String sequence = String.format("%03d", counter.incrementAndGet() % 1000);

    return String.join(
        ID_SEPARATOR, "BCH", productToken, batchType.toUpperCase(), instanceId, date, sequence);
  }

  /** Extracts product code from any supported ID format. */
  public String extractProductCode(String generatedId) {
    if (generatedId == null || !generatedId.contains(ID_SEPARATOR)) {
      return null;
    }

    String[] parts = generatedId.split(ID_SEPARATOR);
    // Prefix-aware parsing to support multiple ID schemas without ambiguity.
    // Schemas:
    //  - Business Txn:   <PRODUCT>-<ENV>-<TIMESTAMP>-<INSTANCE>-<COUNTER>-<RAND>
    //  - Correlation:    COR-<PRODUCT>-<INSTANCE>-<UUID8>-<NANOTIME>
    //  - Session:        SES-<PRODUCT>-<INSTANCE>-<USERHASH>-<TS8>
    //  - Trace:          TRC-<PRODUCT>-<INSTANCE>-<TIMESTAMP>-<RAND8>
    //  - Batch:          BCH-<PRODUCT>-<BATCHTYPE>-<INSTANCE>-<DATE>-<SEQ>
    String prefix = parts[0];
    switch (prefix) {
      case "COR":
      case "SES":
      case "TRC":
      case "BCH":
        return parts.length > 1 ? parts[1] : null;
      default:
        // Assume this is a Business Transaction ID where the first token is the product code
        return parts[0];
    }
  }

  /**
   * Expose configured product code to align other components (e.g., aspect) without duplication.
   */
  public String getProductCode() {
    return productCode;
  }

  /** Expose active environment (profile) used in ID composition for debugging/consistency. */
  public String getEnvironment() {
    return environment;
  }

  /**
   * Normalized product token used in IDs (non-alphanumerics replaced with underscore, uppercased).
   */
  public String getProductToken() {
    return productToken;
  }

  /** Resets the counter (useful for testing) */
  public void resetCounter() {
    counter.set(0);
  }

  /** Gets current counter value (useful for monitoring) */
  public long getCurrentCounter() {
    return counter.get();
  }

  /** Gets the unique instance ID for this application instance. */
  public String getInstanceId() {
    return instanceId;
  }

  /**
   * Creates a salted SHA-256 hash of the input string.
   *
   * @param input The string to hash.
   * @param salt The salt to use for hashing.
   * @return A hex representation of the salted hash.
   */
  private String secureHash(String input, String salt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      // Apply salt
      digest.update(salt.getBytes(StandardCharsets.UTF_8));
      byte[] hashedBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

      // Convert byte array to hex string
      StringBuilder hexString = new StringBuilder(2 * hashedBytes.length);
      for (byte b : hashedBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      // Return a shortened version of the hash for brevity in IDs
      return hexString.toString().substring(0, 16).toUpperCase();
    } catch (NoSuchAlgorithmException e) {
      // This should never happen as SHA-256 is a standard algorithm
      // Fallback to a non-secure hash to avoid crashing the application
      logger.warn(
          "SHA-256 algorithm not found. Falling back to non-secure hashCode for user ID hashing.",
          e);
      return Integer.toHexString(Math.abs(input.hashCode())).toUpperCase();
    }
  }
}
