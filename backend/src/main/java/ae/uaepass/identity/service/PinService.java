package ae.uaepass.identity.service;

import ae.uaepass.identity.config.AppSecurityProperties;
import ae.uaepass.identity.entity.*;
import ae.uaepass.identity.repository.CredentialRepository;
import ae.uaepass.identity.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * PIN creation and verification service.
 * 
 * SECURITY DECISIONS:
 * - Argon2id: memory-hard, resists GPU/ASIC attacks
 * - Parameters: memory=65536 KB (64 MB), iterations=3, parallelism=1
 * - Server pepper appended before hashing (from env var)
 * - Per-user salt generated via SecureRandom (16 bytes)
 * - PIN never logged, never stored in plaintext
 * - PIN complexity enforced (6 digits, no sequential/repeated patterns)
 */
@Service
public class PinService {

    private static final int ARGON2_MEMORY = 65536;  // 64 MB
    private static final int ARGON2_ITERATIONS = 3;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int ARGON2_HASH_LENGTH = 32; // bytes
    private static final int SALT_LENGTH = 16;        // bytes

    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final String serverPepper;
    private final SecureRandom secureRandom;

    public PinService(CredentialRepository credentialRepository,
                      UserRepository userRepository,
                      AuditService auditService,
                      AppSecurityProperties securityProps) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.serverPepper = securityProps.crypto().serverPepper();
        this.secureRandom = new SecureRandom();
    }

    /**
     * Create PIN for user. The PIN is hashed with Argon2id + server pepper before storage.
     *
     * @throws IllegalArgumentException if PIN doesn't meet complexity requirements
     * @throws IllegalStateException if user not in OTP_VERIFIED status
     */
    @Transactional
    public void createPin(UUID userId, String pinPlaintext, HttpServletRequest request) {
        validatePinComplexity(pinPlaintext);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getStatus() != UserStatus.OTP_VERIFIED) {
            throw new IllegalStateException("OTP must be verified before creating PIN");
        }

        // Check for existing credential
        if (credentialRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("PIN already set for this user");
        }

        // Hash PIN: Argon2id(pin + serverPepper, salt)
        String pinWithPepper = pinPlaintext + serverPepper;
        String hashResult = hashWithArgon2id(pinWithPepper);

        Credential credential = new Credential();
        credential.setUser(user);
        credential.setPinHash(hashResult);
        credential.setHashAlgorithm("ARGON2ID");
        credentialRepository.save(credential);

        // Activate user account
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        auditService.logEvent(AuditEventType.PIN_CREATED, userId, request);
    }

    /**
     * Verify PIN for authentication.
     */
    public boolean verifyPin(UUID userId, String pinPlaintext) {
        Credential credential = credentialRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("No credentials found"));

        String pinWithPepper = pinPlaintext + serverPepper;
        return verifyArgon2id(pinWithPepper, credential.getPinHash());
    }

    /**
     * Validate PIN complexity rules:
     * - Exactly 6 digits
     * - No sequential patterns (123456, 654321)
     * - No repeated patterns (111111, 222222)
     */
    private void validatePinComplexity(String pin) {
        if (pin == null || !pin.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("PIN must be exactly 6 digits");
        }

        // Check for all same digits
        if (pin.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("PIN cannot be all the same digit");
        }

        // Check for sequential ascending (123456)
        boolean sequential = true;
        for (int i = 1; i < pin.length(); i++) {
            if (pin.charAt(i) - pin.charAt(i - 1) != 1) {
                sequential = false;
                break;
            }
        }
        if (sequential) {
            throw new IllegalArgumentException("PIN cannot be a sequential pattern");
        }

        // Check for sequential descending (654321)
        sequential = true;
        for (int i = 1; i < pin.length(); i++) {
            if (pin.charAt(i) - pin.charAt(i - 1) != -1) {
                sequential = false;
                break;
            }
        }
        if (sequential) {
            throw new IllegalArgumentException("PIN cannot be a sequential pattern");
        }
    }

    private String hashWithArgon2id(String input) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withMemoryAsKB(ARGON2_MEMORY)
            .withIterations(ARGON2_ITERATIONS)
            .withParallelism(ARGON2_PARALLELISM)
            .withSalt(salt)
            .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] hash = new byte[ARGON2_HASH_LENGTH];
        generator.generateBytes(input.getBytes(StandardCharsets.UTF_8), hash);

        // Store as: $argon2id$salt$hash (all base64)
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);
        return "$argon2id$" + saltB64 + "$" + hashB64;
    }

    private boolean verifyArgon2id(String input, String stored) {
        // Parse stored format: $argon2id$salt$hash
        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withMemoryAsKB(ARGON2_MEMORY)
            .withIterations(ARGON2_ITERATIONS)
            .withParallelism(ARGON2_PARALLELISM)
            .withSalt(salt)
            .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] hash = new byte[ARGON2_HASH_LENGTH];
        generator.generateBytes(input.getBytes(StandardCharsets.UTF_8), hash);

        // Constant-time comparison to prevent timing attacks
        return java.security.MessageDigest.isEqual(hash, expectedHash);
    }
}
