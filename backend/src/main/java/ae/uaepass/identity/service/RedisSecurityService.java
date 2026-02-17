package ae.uaepass.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-backed security state for real-time enforcement.
 *
 * SECURITY CONTRACT:
 * - All auth-critical checks FAIL CLOSED if Redis is unavailable
 * - OTP attempt counters auto-expire with TTL
 * - Token replay detection uses SET with expiry matching token TTL
 * - Lockout keys have configurable durations
 */
@Service
public class RedisSecurityService {

    private static final Logger log = LoggerFactory.getLogger(RedisSecurityService.class);

    private static final String KEY_OTP_ATTEMPTS = "security:otp:attempts:";
    private static final String KEY_LOCKOUT = "security:lockout:";
    private static final String KEY_TOKEN_USED = "security:token:used:";
    private static final String KEY_SESSION_ANOMALY = "security:anomaly:";
    private static final String KEY_PIN_ATTEMPTS = "security:pin:attempts:";

    private static final Duration OTP_ATTEMPT_WINDOW = Duration.ofMinutes(10);
    private static final Duration DEFAULT_LOCKOUT = Duration.ofMinutes(30);
    private static final int MAX_OTP_ATTEMPTS = 10;
    private static final int MAX_PIN_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;

    public RedisSecurityService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ============================
    // OTP Attempt Tracking
    // ============================

    /**
     * Increment OTP attempt counter. Returns current count.
     * Fails closed (throws) if Redis unavailable.
     */
    public long incrementOtpAttempt(UUID userId) {
        try {
            String key = KEY_OTP_ATTEMPTS + userId;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, OTP_ATTEMPT_WINDOW);
            }
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis unavailable for OTP attempt tracking — FAILING CLOSED", e);
            throw new SecurityException("Security service unavailable. Please try again later.");
        }
    }

    public long getOtpAttemptCount(UUID userId) {
        try {
            String val = redisTemplate.opsForValue().get(KEY_OTP_ATTEMPTS + userId);
            return val != null ? Long.parseLong(val) : 0;
        } catch (Exception e) {
            log.error("Redis unavailable for OTP attempt check — FAILING CLOSED", e);
            throw new SecurityException("Security service unavailable. Please try again later.");
        }
    }

    public boolean isOtpRateLimited(UUID userId) {
        return getOtpAttemptCount(userId) >= MAX_OTP_ATTEMPTS;
    }

    // ============================
    // PIN Attempt Tracking
    // ============================

    public long incrementPinAttempt(UUID userId) {
        try {
            String key = KEY_PIN_ATTEMPTS + userId;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(15));
            }
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis unavailable for PIN attempt tracking — FAILING CLOSED", e);
            throw new SecurityException("Security service unavailable. Please try again later.");
        }
    }

    public boolean isPinLocked(UUID userId) {
        try {
            String val = redisTemplate.opsForValue().get(KEY_PIN_ATTEMPTS + userId);
            return val != null && Long.parseLong(val) >= MAX_PIN_ATTEMPTS;
        } catch (Exception e) {
            throw new SecurityException("Security service unavailable.");
        }
    }

    public void clearPinAttempts(UUID userId) {
        try {
            redisTemplate.delete(KEY_PIN_ATTEMPTS + userId);
        } catch (Exception e) {
            log.warn("Failed to clear PIN attempts in Redis", e);
        }
    }

    // ============================
    // Account Lockout
    // ============================

    public void setLockout(UUID userId, Duration duration) {
        try {
            redisTemplate.opsForValue().set(
                KEY_LOCKOUT + userId, "LOCKED", duration != null ? duration : DEFAULT_LOCKOUT
            );
        } catch (Exception e) {
            log.error("Redis unavailable for lockout — FAILING CLOSED", e);
            throw new SecurityException("Security service unavailable.");
        }
    }

    public boolean isLockedOut(UUID userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_LOCKOUT + userId));
        } catch (Exception e) {
            log.error("Redis unavailable for lockout check — FAILING CLOSED", e);
            throw new SecurityException("Security service unavailable.");
        }
    }

    public void clearLockout(UUID userId) {
        try {
            redisTemplate.delete(KEY_LOCKOUT + userId);
        } catch (Exception e) {
            log.warn("Failed to clear lockout in Redis", e);
        }
    }

    // ============================
    // Token Replay Detection
    // ============================

    /**
     * Mark a token (by JTI) as used. TTL matches token expiry.
     * For one-time-use refresh tokens.
     */
    public void markTokenUsed(String jti, Duration tokenTtl) {
        try {
            redisTemplate.opsForValue().set(KEY_TOKEN_USED + jti, "1", tokenTtl);
        } catch (Exception e) {
            log.error("Redis unavailable for token tracking — FAILING CLOSED", e);
            throw new SecurityException("Security service unavailable.");
        }
    }

    public boolean isTokenReplayed(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_TOKEN_USED + jti));
        } catch (Exception e) {
            log.error("Redis unavailable for replay check — FAILING CLOSED", e);
            throw new SecurityException("Security service unavailable.");
        }
    }

    // ============================
    // Session Anomaly Flags
    // ============================

    public void flagSessionAnomaly(UUID userId, String reason) {
        try {
            redisTemplate.opsForValue().set(
                KEY_SESSION_ANOMALY + userId, reason, Duration.ofHours(1)
            );
        } catch (Exception e) {
            log.warn("Failed to flag session anomaly in Redis", e);
        }
    }

    public String getSessionAnomalyFlag(UUID userId) {
        try {
            return redisTemplate.opsForValue().get(KEY_SESSION_ANOMALY + userId);
        } catch (Exception e) {
            return null; // Anomaly flags are advisory, don't fail closed
        }
    }

    public void clearSessionAnomaly(UUID userId) {
        try {
            redisTemplate.delete(KEY_SESSION_ANOMALY + userId);
        } catch (Exception e) {
            log.warn("Failed to clear session anomaly flag", e);
        }
    }
}
