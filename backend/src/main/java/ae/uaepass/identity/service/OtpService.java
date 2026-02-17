package ae.uaepass.identity.service;

import ae.uaepass.identity.config.AppSecurityProperties;
import ae.uaepass.identity.entity.*;
import ae.uaepass.identity.repository.OtpChallengeRepository;
import ae.uaepass.identity.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * OTP generation, delivery, and verification service.
 * 
 * SECURITY DECISIONS:
 * - OTP hashed with SHA-256 before storage (never plaintext)
 * - OTP expires in 3 minutes (configurable)
 * - Max 5 verification attempts per challenge
 * - 60-second resend cooldown
 * - Account locked after 3 consecutive failed OTP cycles
 * - OTP value NEVER appears in any log
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpChallengeRepository otpChallengeRepository;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;
    private final AuditService auditService;
    private final AppSecurityProperties.OtpProperties otpProps;

    public OtpService(OtpChallengeRepository otpChallengeRepository,
                      UserRepository userRepository,
                      CryptoService cryptoService,
                      AuditService auditService,
                      AppSecurityProperties securityProps) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
        this.auditService = auditService;
        this.otpProps = securityProps.otp();
    }

    /**
     * Generate and store a new OTP challenge for the user.
     * Returns the plaintext OTP for delivery via the notification service.
     *
     * @throws IllegalStateException if resend cooldown not elapsed
     * @throws SecurityException if account is locked due to too many failures
     */
    @Transactional
    public String generateOtp(UUID userId, OtpChannel channel, HttpServletRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if account should be locked
        long failedCycles = otpChallengeRepository.countRecentFailedCycles(
            userId, Instant.now().minus(Duration.ofHours(1))
        );
        if (failedCycles >= otpProps.maxCyclesBeforeLock()) {
            user.setStatus(UserStatus.LOCKED);
            userRepository.save(user);
            auditService.logEvent(AuditEventType.ACCOUNT_LOCKED, userId, request,
                Map.of("reason", "max_otp_cycles_exceeded"));
            throw new SecurityException("Account locked due to too many failed OTP attempts");
        }

        // Enforce resend cooldown
        otpChallengeRepository.findLatestByUserIdAndChannel(userId, channel)
            .ifPresent(latest -> {
                Duration sinceLastSend = Duration.between(latest.getCreatedAt(), Instant.now());
                if (sinceLastSend.getSeconds() < otpProps.resendCooldownSeconds()) {
                    long remaining = otpProps.resendCooldownSeconds() - sinceLastSend.getSeconds();
                    throw new IllegalStateException("Resend cooldown active. Wait " + remaining + " seconds.");
                }
            });

        // Generate OTP
        String otpPlaintext = cryptoService.generateOtp(otpProps.length());
        String otpHash = cryptoService.hashRaw(otpPlaintext);

        // Store hashed OTP challenge
        OtpChallenge challenge = new OtpChallenge();
        challenge.setUser(user);
        challenge.setOtpHash(otpHash);
        challenge.setChannel(channel);
        challenge.setExpiresAt(Instant.now().plusSeconds(otpProps.expirySeconds()));
        challenge.setMaxAttempts(otpProps.maxAttempts());
        otpChallengeRepository.save(challenge);

        // Update user status
        user.setStatus(UserStatus.OTP_SENT);
        userRepository.save(user);

        auditService.logEvent(AuditEventType.OTP_SENT, userId, request,
            Map.of("channel", channel.name()));

        // SECURITY: Return plaintext OTP only for notification delivery.
        // It must NOT be logged or stored anywhere else.
        return otpPlaintext;
    }

    /**
     * Verify an OTP code against the active challenge.
     *
     * @return true if OTP is valid
     * @throws IllegalStateException if no active OTP or max attempts reached
     */
    @Transactional
    public boolean verifyOtp(UUID userId, OtpChannel channel, String otpCode, HttpServletRequest request) {
        OtpChallenge challenge = otpChallengeRepository
            .findActiveByUserIdAndChannel(userId, channel, Instant.now())
            .orElseThrow(() -> new IllegalStateException("No active OTP challenge found. Please request a new OTP."));

        // Check max attempts
        if (challenge.isMaxAttemptsReached()) {
            auditService.logEvent(AuditEventType.OTP_FAILED, userId, request,
                Map.of("reason", "max_attempts_reached"));
            throw new IllegalStateException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        // Check expiry
        if (challenge.isExpired()) {
            auditService.logEvent(AuditEventType.OTP_FAILED, userId, request,
                Map.of("reason", "expired"));
            throw new IllegalStateException("OTP has expired. Please request a new OTP.");
        }

        // Verify hash
        String inputHash = cryptoService.hashRaw(otpCode);
        challenge.setAttempts(challenge.getAttempts() + 1);

        if (inputHash.equals(challenge.getOtpHash())) {
            // Success: mark consumed
            challenge.setConsumed(true);
            otpChallengeRepository.save(challenge);

            // Update user status
            User user = challenge.getUser();
            user.setStatus(UserStatus.OTP_VERIFIED);
            userRepository.save(user);

            auditService.logEvent(AuditEventType.OTP_VERIFIED, userId, request);
            return true;
        } else {
            // Failure: increment attempt counter
            otpChallengeRepository.save(challenge);
            auditService.logEvent(AuditEventType.OTP_FAILED, userId, request,
                Map.of("attemptsUsed", challenge.getAttempts(), "maxAttempts", challenge.getMaxAttempts()));
            return false;
        }
    }
}
