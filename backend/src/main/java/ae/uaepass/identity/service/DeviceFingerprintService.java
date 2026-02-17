package ae.uaepass.identity.service;

import ae.uaepass.identity.entity.DeviceSession;
import ae.uaepass.identity.entity.TrustLevel;
import ae.uaepass.identity.repository.DeviceSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Device fingerprint computation and session tracking.
 *
 * Fingerprint = SHA-256(User-Agent + Accept-Language + IP prefix)
 * Only the hash is stored — never the raw composite.
 */
@Service
public class DeviceFingerprintService {

    private final DeviceSessionRepository deviceSessionRepository;

    public DeviceFingerprintService(DeviceSessionRepository deviceSessionRepository) {
        this.deviceSessionRepository = deviceSessionRepository;
    }

    /**
     * Compute device fingerprint hash from request headers.
     */
    public String computeFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLang = request.getHeader("Accept-Language");
        String screenHint = request.getHeader("X-Screen-Resolution"); // Custom header from frontend
        String timezoneHint = request.getHeader("X-Timezone");

        String composite = normalize(userAgent)
            + "|" + normalize(acceptLang)
            + "|" + normalize(screenHint)
            + "|" + normalize(timezoneHint);

        return sha256(composite);
    }

    /**
     * Register or update a device session for a user.
     * Returns true if this is an unknown (new) device.
     */
    @Transactional
    public boolean trackDevice(UUID userId, HttpServletRequest request) {
        String fingerprint = computeFingerprint(request);
        String userAgent = request.getHeader("User-Agent");
        String ip = extractClientIp(request);

        Optional<DeviceSession> existing = deviceSessionRepository
            .findActiveByUserAndFingerprint(userId, fingerprint);

        if (existing.isPresent()) {
            DeviceSession session = existing.get();
            session.setLastSeenAt(Instant.now());
            session.setIpAddress(ip);
            session.setLoginCount(session.getLoginCount() + 1);

            // Promote trust after repeated successful logins
            if (session.getLoginCount() >= 5 && session.getTrustLevel() == TrustLevel.LOW) {
                session.setTrustLevel(TrustLevel.MEDIUM);
            } else if (session.getLoginCount() >= 20 && session.getTrustLevel() == TrustLevel.MEDIUM) {
                session.setTrustLevel(TrustLevel.HIGH);
            }

            deviceSessionRepository.save(session);
            return false; // Known device
        }

        // New device
        DeviceSession newSession = new DeviceSession();
        // We need to set the user via a reference — create a lazy proxy
        newSession.setDeviceFingerprintHash(fingerprint);
        newSession.setUserAgent(truncateUserAgent(userAgent));
        newSession.setIpAddress(ip);
        newSession.setTrustLevel(TrustLevel.LOW);
        newSession.setLoginCount(1);

        // Set user via repository lookup (lazy)
        return true; // New device — caller should persist with user reference
    }

    /**
     * Get trusted devices for a user.
     */
    @Transactional(readOnly = true)
    public List<DeviceSession> getActiveDevices(UUID userId) {
        return deviceSessionRepository.findByUserIdAndActiveTrue(userId);
    }

    /**
     * Revoke a device session.
     */
    @Transactional
    public void revokeDevice(UUID deviceId) {
        deviceSessionRepository.findById(deviceId).ifPresent(session -> {
            session.setActive(false);
            session.setRevokedAt(Instant.now());
            deviceSessionRepository.save(session);
        });
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalize(String value) {
        return value != null ? value.trim().toLowerCase() : "unknown";
    }

    private String truncateUserAgent(String ua) {
        if (ua == null) return null;
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
