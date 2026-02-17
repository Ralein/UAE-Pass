package ae.uaepass.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisSecurityService.
 * Tests OTP tracking, lockout, token replay detection, and fail-closed
 * behavior.
 */
@ExtendWith(MockitoExtension.class)
class RedisSecurityServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RedisSecurityService service;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new RedisSecurityService(redisTemplate);
        testUserId = UUID.randomUUID();
    }

    @Test
    void incrementOtpAttempt_firstAttempt_setsExpiry() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        long count = service.incrementOtpAttempt(testUserId);

        assertEquals(1, count);
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void incrementOtpAttempt_subsequentAttempt_noExpiry() {
        when(valueOps.increment(anyString())).thenReturn(3L);

        long count = service.incrementOtpAttempt(testUserId);

        assertEquals(3, count);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void isOtpRateLimited_underLimit_returnsFalse() {
        when(valueOps.get(anyString())).thenReturn("5");

        assertFalse(service.isOtpRateLimited(testUserId));
    }

    @Test
    void isOtpRateLimited_atLimit_returnsTrue() {
        when(valueOps.get(anyString())).thenReturn("10");

        assertTrue(service.isOtpRateLimited(testUserId));
    }

    @Test
    void isLockedOut_noKey_returnsFalse() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertFalse(service.isLockedOut(testUserId));
    }

    @Test
    void isLockedOut_keyExists_returnsTrue() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertTrue(service.isLockedOut(testUserId));
    }

    @Test
    void setLockout_setsKeyWithTtl() {
        service.setLockout(testUserId, Duration.ofMinutes(30));

        verify(valueOps).set(contains("lockout"), eq("LOCKED"), eq(Duration.ofMinutes(30)));
    }

    @Test
    void markTokenUsed_setsKeyWithTtl() {
        String jti = "test-jti-12345";
        Duration ttl = Duration.ofMinutes(15);

        service.markTokenUsed(jti, ttl);

        verify(valueOps).set(contains(jti), eq("1"), eq(ttl));
    }

    @Test
    void isTokenReplayed_notUsed_returnsFalse() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertFalse(service.isTokenReplayed("fresh-jti"));
    }

    @Test
    void isTokenReplayed_alreadyUsed_returnsTrue() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertTrue(service.isTokenReplayed("replayed-jti"));
    }

    @Test
    void failClosed_redisUnavailable_throwsSecurityException() {
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(SecurityException.class, () -> service.incrementOtpAttempt(testUserId));
    }

    @Test
    void failClosed_lockoutCheckUnavailable_throwsSecurityException() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(SecurityException.class, () -> service.isLockedOut(testUserId));
    }

    @Test
    void failClosed_replayCheckUnavailable_throwsSecurityException() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(SecurityException.class, () -> service.isTokenReplayed("any-jti"));
    }
}
