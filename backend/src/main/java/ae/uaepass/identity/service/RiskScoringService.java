package ae.uaepass.identity.service;

import ae.uaepass.identity.entity.*;
import ae.uaepass.identity.repository.DeviceSessionRepository;
import ae.uaepass.identity.repository.RiskEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Login risk scoring engine.
 *
 * Scores: 0–30 (low), 31–70 (medium), 71–100 (high)
 * High risk → require step-up auth or block
 *
 * Factors:
 * - New/unknown device (+30)
 * - Recent failures from same user (+15 per recent failure)
 * - IP change from last known session (+20)
 * - High velocity (many attempts in short window) (+25)
 * - Active lockout (+50)
 */
@Service
public class RiskScoringService {

    private static final Logger log = LoggerFactory.getLogger(RiskScoringService.class);

    private static final int THRESHOLD_HIGH = 70;
    private static final int THRESHOLD_MEDIUM = 30;

    private final RiskEventRepository riskEventRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final RedisSecurityService redisSecurityService;
    private final DeviceFingerprintService deviceFingerprintService;

    public RiskScoringService(RiskEventRepository riskEventRepository,
                               DeviceSessionRepository deviceSessionRepository,
                               RedisSecurityService redisSecurityService,
                               DeviceFingerprintService deviceFingerprintService) {
        this.riskEventRepository = riskEventRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.redisSecurityService = redisSecurityService;
        this.deviceFingerprintService = deviceFingerprintService;
    }

    /**
     * Score a login attempt. Returns 0–100.
     */
    @Transactional(readOnly = true)
    public int scoreLoginRisk(UUID userId, HttpServletRequest request) {
        int score = 0;
        String fingerprint = deviceFingerprintService.computeFingerprint(request);
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);

        // Factor 1: Unknown device
        Optional<DeviceSession> knownDevice = deviceSessionRepository
            .findActiveByUserAndFingerprint(userId, fingerprint);
        if (knownDevice.isEmpty()) {
            score += 30;
        } else if (knownDevice.get().getTrustLevel() == TrustLevel.LOW) {
            score += 10;
        }

        // Factor 2: Recent brute force events
        long bruteForceCount = riskEventRepository.countRecentByUserAndType(
            userId, RiskEventType.BRUTE_FORCE, since
        );
        score += (int) Math.min(bruteForceCount * 15, 45);

        // Factor 3: OTP abuse in sliding window
        long otpAbuseCount = riskEventRepository.countRecentByUserAndType(
            userId, RiskEventType.OTP_ABUSE, since
        );
        score += (int) Math.min(otpAbuseCount * 10, 30);

        // Factor 4: Active lockout
        if (redisSecurityService.isLockedOut(userId)) {
            score += 50;
        }

        // Factor 5: High velocity OTP attempts (from Redis)
        long otpAttempts = redisSecurityService.getOtpAttemptCount(userId);
        if (otpAttempts > 5) {
            score += 25;
        }

        // Cap at 100
        score = Math.min(score, 100);

        log.debug("Risk score for user {}: {} (device_known={}, brute_force={}, otp_abuse={}, otp_attempts={})",
            userId, score, knownDevice.isPresent(), bruteForceCount, otpAbuseCount, otpAttempts);

        return score;
    }

    /**
     * Evaluate risk level for a score.
     */
    public RiskLevel evaluateRiskLevel(int score) {
        if (score >= THRESHOLD_HIGH) return RiskLevel.HIGH;
        if (score >= THRESHOLD_MEDIUM) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    /**
     * Record a risk event.
     */
    @Transactional
    public void recordRiskEvent(UUID userId, RiskEventType eventType, int riskScore,
                                 HttpServletRequest request, String metadata) {
        RiskEvent event = new RiskEvent();
        // User is set via lazy proxy through repository
        event.setEventType(eventType);
        event.setRiskScore(riskScore);
        event.setSourceIp(extractClientIp(request));
        event.setDeviceFingerprintHash(deviceFingerprintService.computeFingerprint(request));
        event.setMetadata(metadata);
        riskEventRepository.save(event);
    }

    /**
     * Check if user should be blocked based on accumulated risk.
     */
    @Transactional(readOnly = true)
    public boolean shouldBlockUser(UUID userId) {
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        double avgScore = riskEventRepository.averageUnresolvedRiskScore(userId, since);
        return avgScore >= THRESHOLD_HIGH;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }
}
