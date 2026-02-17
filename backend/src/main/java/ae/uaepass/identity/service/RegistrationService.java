package ae.uaepass.identity.service;

import ae.uaepass.identity.config.AppSecurityProperties;
import ae.uaepass.identity.dto.RegistrationStartRequest;
import ae.uaepass.identity.entity.*;
import ae.uaepass.identity.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Multi-step user registration service.
 * 
 * Flow: startRegistration → (OTP via OtpService) → (PIN via PinService) → ACTIVE
 * 
 * SECURITY: All PII is hashed/encrypted before database storage.
 * Emirates ID, email, and phone are SHA-256 hashed for lookup.
 * Full name is AES-GCM encrypted (needs to be decryptable for user profile).
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository userRepository;
    private final CryptoService cryptoService;
    private final OtpService otpService;
    private final AuditService auditService;

    public RegistrationService(UserRepository userRepository,
                                CryptoService cryptoService,
                                OtpService otpService,
                                AuditService auditService) {
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
        this.otpService = otpService;
        this.auditService = auditService;
    }

    /**
     * Start registration: validate identity, hash PII, create user record, send OTP.
     *
     * @return userId for subsequent steps
     * @throws IllegalStateException if Emirates ID already registered
     */
    @Transactional
    public UUID startRegistration(RegistrationStartRequest request, HttpServletRequest httpRequest) {
        // Hash all identifiers for storage and duplicate detection
        String emiratesIdHash = cryptoService.hash(request.emiratesId());
        String emailHash = cryptoService.hash(request.email());
        String phoneHash = cryptoService.hash(request.phone());

        // Check for duplicate Emirates ID
        if (userRepository.existsByEmiratesIdHash(emiratesIdHash)) {
            throw new IllegalStateException("An account with this Emirates ID already exists");
        }

        // Encrypt PII that needs to be decryptable
        String fullNameEnc = cryptoService.encrypt(request.fullName());

        // Create user record
        User user = new User();
        user.setEmiratesIdHash(emiratesIdHash);
        user.setEmailHash(emailHash);
        user.setPhoneHash(phoneHash);
        user.setFullNameEnc(fullNameEnc);
        user.setGender(request.gender());
        user.setStatus(UserStatus.PENDING);
        user.setAccountLevel(AccountLevel.SOP1);

        user = userRepository.save(user);

        auditService.logEvent(AuditEventType.REGISTRATION_START, user.getId(), httpRequest,
            Map.of("accountLevel", "SOP1"));

        // Auto-send OTP to phone
        otpService.generateOtp(user.getId(), OtpChannel.SMS, httpRequest);

        return user.getId();
    }

    /**
     * Get user registration status.
     */
    @Transactional(readOnly = true)
    public UserStatus getRegistrationStatus(UUID userId) {
        return userRepository.findById(userId)
            .map(User::getStatus)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
