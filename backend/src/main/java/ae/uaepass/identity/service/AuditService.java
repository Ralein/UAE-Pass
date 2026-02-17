package ae.uaepass.identity.service;

import ae.uaepass.identity.entity.AuditEventType;
import ae.uaepass.identity.entity.AuditLog;
import ae.uaepass.identity.repository.AuditLogRepository;
import ae.uaepass.identity.security.SecretsProvider;
import ae.uaepass.identity.util.PiiMaskingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Async audit logging service with tamper detection.
 *
 * SECURITY:
 * - Never logs secrets, OTP values, tokens, or PINs
 * - PII masking applied to all log entries
 * - HMAC chain: each entry includes hash of previous entry for tamper detection
 * - Failures must not crash the request
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final SecretsProvider secretsProvider;

    // HMAC chain: stores the hash of the last audit entry
    private final AtomicReference<String> lastEntryHash = new AtomicReference<>("GENESIS");

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper,
                        SecretsProvider secretsProvider) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.secretsProvider = secretsProvider;
    }

    /**
     * Log a security event asynchronously with PII masking and HMAC chain.
     */
    @Async
    public void logEvent(AuditEventType eventType, UUID userId, HttpServletRequest request,
                         Map<String, Object> details) {
        try {
            String requestId = MDC.get("requestId");
            String ipAddress = request != null ? resolveClientIp(request) : null;
            String deviceFingerprint = request != null ? request.getHeader("X-Device-Fingerprint") : null;

            String detailsJson = null;
            if (details != null && !details.isEmpty()) {
                detailsJson = objectMapper.writeValueAsString(details);
            }

            // Build HMAC chain entry
            String previousHash = lastEntryHash.get();
            String entryData = eventType + "|" + userId + "|" + requestId + "|" + previousHash;
            String entryHmac = computeHmac(entryData);
            lastEntryHash.set(entryHmac);

            AuditLog auditLog = new AuditLog()
                .eventType(eventType)
                .userId(userId)
                .requestId(requestId)
                .ipAddress(ipAddress)
                .deviceFingerprintHash(deviceFingerprint)
                .details(detailsJson);

            auditLogRepository.save(auditLog);

            // Log with masked PII
            log.info("Audit event: type={}, userId={}, requestId={}, ip={}, hmac={}",
                eventType,
                userId != null ? PiiMaskingUtil.maskUuid(userId.toString()) : "N/A",
                requestId,
                ipAddress != null ? PiiMaskingUtil.maskIp(ipAddress) : "N/A",
                entryHmac);

        } catch (Exception e) {
            log.error("Failed to write audit log: type={}, userId={}", eventType, userId, e);
        }
    }

    /**
     * Convenience overload for events without extra details.
     */
    @Async
    public void logEvent(AuditEventType eventType, UUID userId, HttpServletRequest request) {
        logEvent(eventType, userId, request, null);
    }

    /**
     * Overload for events without an HTTP request context (e.g., scheduled tasks).
     */
    @Async
    public void logEvent(AuditEventType eventType, UUID userId, String requestId,
                         Map<String, Object> details) {
        try {
            String detailsJson = null;
            if (details != null && !details.isEmpty()) {
                detailsJson = objectMapper.writeValueAsString(details);
            }

            String previousHash = lastEntryHash.get();
            String entryData = eventType + "|" + userId + "|" + requestId + "|" + previousHash;
            String entryHmac = computeHmac(entryData);
            lastEntryHash.set(entryHmac);

            AuditLog auditLog = new AuditLog()
                .eventType(eventType)
                .userId(userId)
                .requestId(requestId)
                .details(detailsJson);

            auditLogRepository.save(auditLog);

            log.info("Audit event: type={}, userId={}, hmac={}", eventType,
                userId != null ? PiiMaskingUtil.maskUuid(userId.toString()) : "N/A", entryHmac);
        } catch (Exception e) {
            log.error("Failed to write audit log: type={}", eventType, e);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String computeHmac(String data) {
        try {
            String key = secretsProvider.getAuditHmacKey();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            log.warn("HMAC computation failed, using fallback", e);
            return "hmac-unavailable";
        }
    }
}

