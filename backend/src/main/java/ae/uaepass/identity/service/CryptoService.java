package ae.uaepass.identity.service;

import ae.uaepass.identity.config.AppSecurityProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cryptographic utility service.
 * 
 * SECURITY DECISIONS:
 * - SHA-256 for identifier hashing (with app salt): irreversible lookup keys
 * - AES-256-GCM for PII encryption: authenticated encryption with unique IV per operation
 * - All keys/salts from environment variables, never hardcoded
 * - Uses java.security and javax.crypto only — no custom crypto
 */
@Service
public class CryptoService {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12;   // bytes

    private final String hashSalt;
    private final byte[] aesKeyBytes;
    private final SecureRandom secureRandom;

    public CryptoService(AppSecurityProperties securityProps) {
        this.hashSalt = securityProps.crypto().hashSalt();
        this.aesKeyBytes = Base64.getDecoder().decode(securityProps.crypto().aesKey());
        this.secureRandom = new SecureRandom();

        // Validate AES key length on startup
        if (this.aesKeyBytes.length != 32) {
            throw new IllegalStateException("AES key must be exactly 32 bytes (256 bits), got " + this.aesKeyBytes.length);
        }
    }

    /**
     * SHA-256 hash with application salt.
     * Used for: Emirates ID, email, phone number — creating irreversible lookup keys.
     */
    public String hash(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = hashSalt + ":" + plaintext;
            byte[] hashBytes = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * SHA-256 hash without salt.
     * Used for: OTP hashing (ephemeral, no need for persistent salt).
     */
    public String hashRaw(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * AES-256-GCM encryption.
     * Used for: full name and other PII fields that need to be decrypted.
     * Returns: Base64(IV || ciphertext || GCM tag)
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * AES-256-GCM decryption.
     * Input: Base64(IV || ciphertext || GCM tag)
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generate OTP of specified length using SecureRandom.
     * SECURITY: Uses SecureRandom, not Math.random().
     */
    public String generateOtp(int length) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
}
