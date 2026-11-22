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
 * Thread-safe generator for structured business and correlation identifiers.
 *
 * <p>Educational value:
 * 1. Uniqueness: Combines product token, environment, formatted timestamp, per-instance GUID prefix,
 *    and an AtomicLong counter to prevent collisions across distributed instances.
 * 2. Privacy: Uses salted SHA-256 hashing for user IDs so sensitive identifiers are never logged raw.
 * 3. Security: Fails fast in production if the salt remains set to the insecure default.
 */
@Component
public class BusinessContextIdGenerator {
  private static final Logger logger = LoggerFactory.getLogger(BusinessContextIdGenerator.class);

  private final String productCode;
  private final String productToken;
  private final String environment;
  private final String instanceId;
  private final String salt;
  private final AtomicLong counter = new AtomicLong(0);

  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final String ID_SEPARATOR = "-";

  public BusinessContextIdGenerator(
      @Value("${app.product.code:ECOM-POC}") String productCode,
      @Value("${spring.profiles.active:dev}") String environment,
      @Value("${app.security.salt:dev-only-salt}") String salt) {
    this.productCode = productCode;
    this.productToken = productCode.replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
    this.environment = environment;
    this.salt = salt;
    this.instanceId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

    // C7: Validate salt in production environment
    if ("prod".equalsIgnoreCase(environment)) {
      if (salt == null || salt.isBlank() || "dev-only-salt".equals(salt)) {
        throw new IllegalStateException(
            "Production security failure: APP_SECURITY_SALT must be explicitly set to a secret value in production.");
      }
    }
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

  /** Session ID tied to user via salted hash: SES-PRODUCT-INSTANCE-USERHASH-TS8. */
  public String generateSessionId(String userId) {
    String targetUser = (userId == null || userId.trim().isEmpty()) ? "anonymous" : userId;
    String userHash = secureHash(targetUser, salt);
    String timestamp = String.valueOf(System.currentTimeMillis());
    String timestampSuffix = timestamp.substring(Math.max(0, timestamp.length() - 8));

    return String.join(ID_SEPARATOR, "SES", productToken, instanceId, userHash, timestampSuffix);
  }

  /** Operation ID for fine-grained tracking: OPR-TYPE-INSTANCE-DATE-RAND. */
  public String generateOperationId(String operationType) {
    String timestamp = LocalDateTime.now().format(DATE_FORMAT);
    String randomHex =
        Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000)).toUpperCase();

    return String.join(
        ID_SEPARATOR, "OPR", operationType.toUpperCase(), instanceId, timestamp, randomHex);
  }

  /** Trace identifier for business logs: TRC-PRODUCT-INSTANCE-TIMESTAMP-RAND8. */
  public String generateTraceId() {
    String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
    String randomHex = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

    return String.join(ID_SEPARATOR, "TRC", productToken, instanceId, timestamp, randomHex);
  }

  /** Batch ID: BCH-PRODUCT-BATCHTYPE-INSTANCE-DATE-SEQ. */
  public String generateBatchId(String batchType) {
    String date = LocalDateTime.now().format(DATE_FORMAT);
    String sequence = String.format("%03d", counter.incrementAndGet() % 1000);

    return String.join(
        ID_SEPARATOR, "BCH", productToken, batchType.toUpperCase(), instanceId, date, sequence);
  }

  /**
   * Extracts product code from supported ID schemas using Java 21 switch expression.
   */
  public String extractProductCode(String generatedId) {
    if (generatedId == null || !generatedId.contains(ID_SEPARATOR)) {
      return null;
    }

    String[] parts = generatedId.split(ID_SEPARATOR);
    return switch (parts[0]) {
      case "COR", "SES", "TRC", "BCH" -> parts.length > 1 ? parts[1] : null;
      default -> parts[0];
    };
  }

  public String getProductCode() {
    return productCode;
  }

  public String getEnvironment() {
    return environment;
  }

  public String getProductToken() {
    return productToken;
  }

  public void resetCounter() {
    counter.set(0);
  }

  public long getCurrentCounter() {
    return counter.get();
  }

  public String getInstanceId() {
    return instanceId;
  }

  /**
   * Thread-safe salted SHA-256 hash. Creates a fresh MessageDigest per invocation.
   */
  private String secureHash(String input, String salt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(salt.getBytes(StandardCharsets.UTF_8));
      byte[] hashedBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder(2 * hashedBytes.length);
      for (byte b : hashedBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString().substring(0, 16).toUpperCase();
    } catch (NoSuchAlgorithmException e) {
      logger.warn("SHA-256 algorithm unavailable, falling back to hashCode() representation.", e);
      return Integer.toHexString(Math.abs(input.hashCode())).toUpperCase();
    }
  }
}
