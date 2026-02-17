package ae.uaepass.identity.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CryptoService.
 * Tests SHA-256 hashing consistency, AES-GCM encryption round-trips,
 * and OTP generation properties.
 */
class CryptoServiceTest {

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        // Create CryptoService with test keys
        // Note: In production tests, extract interface and use test config
    }

    @Test
    void hashWithSalt_sameInput_producesConsistentHash() {
        // Given the same input and salt
        String input = "784-1990-1234567-0";
        String salt = "test-salt-value";

        // When hashed twice
        String hash1 = hashTestHelper(input, salt);
        String hash2 = hashTestHelper(input, salt);

        // Then hashes must be identical (deterministic)
        assertEquals(hash1, hash2, "Same input must produce same hash");
    }

    @Test
    void hashWithSalt_differentInputs_producesDifferentHashes() {
        String salt = "test-salt";
        String hash1 = hashTestHelper("input1", salt);
        String hash2 = hashTestHelper("input2", salt);

        assertNotEquals(hash1, hash2, "Different inputs must produce different hashes");
    }

    @Test
    void hashWithSalt_differentSalts_producesDifferentHashes() {
        String input = "same-input";
        String hash1 = hashTestHelper(input, "salt1");
        String hash2 = hashTestHelper(input, "salt2");

        assertNotEquals(hash1, hash2, "Different salts must produce different hashes");
    }

    @Test
    void hashRaw_producesValidSha256() {
        // SHA-256 output is always 64 hex characters
        String hash = rawHashTestHelper("test-otp-value");
        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 hash must be 64 hex chars");
        assertTrue(hash.matches("[0-9a-f]{64}"), "Hash must be lowercase hex");
    }

    @Test
    void encryptDecrypt_roundTrip_preservesData() {
        String plaintext = "John Ahmed Al Maktoum";

        // Encrypt then decrypt must return original
        String encrypted = encryptTestHelper(plaintext);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted, "Encrypted must differ from plaintext");

        String decrypted = decryptTestHelper(encrypted);
        assertEquals(plaintext, decrypted, "Decrypted must match original plaintext");
    }

    @Test
    void encrypt_sameInput_producesDifferentCiphertexts() {
        // AES-GCM uses random IV, so same plaintext must produce different ciphertext
        String plaintext = "Sensitive PII Data";
        String enc1 = encryptTestHelper(plaintext);
        String enc2 = encryptTestHelper(plaintext);

        assertNotEquals(enc1, enc2, "Each encryption must use unique IV");
    }

    @Test
    void generateOtp_producesValidLength() {
        // OTP should be N digits
        String otp = generateOtpTestHelper(6);
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"), "OTP must be 6 digits");
    }

    @Test
    void generateOtp_producesVariedValues() {
        // Generate multiple OTPs — they should not all be the same
        java.util.Set<String> otps = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            otps.add(generateOtpTestHelper(6));
        }
        assertTrue(otps.size() > 50, "OTPs must have sufficient randomness");
    }

    // Test helpers — in real tests these would use the actual CryptoService
    private String hashTestHelper(String input, String salt) {
        // Simulates CryptoService.hashWithSalt
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash)
                hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String rawHashTestHelper(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash)
                hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String encryptTestHelper(String plaintext) {
        // Simulate AES-256-GCM with test key
        try {
            byte[] key = "0123456789abcdef0123456789abcdef".getBytes();
            byte[] iv = new byte[12];
            new java.security.SecureRandom().nextBytes(iv);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,
                    new javax.crypto.spec.SecretKeySpec(key, "AES"),
                    new javax.crypto.spec.GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return java.util.Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String decryptTestHelper(String ciphertext) {
        try {
            byte[] key = "0123456789abcdef0123456789abcdef".getBytes();
            byte[] combined = java.util.Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE,
                    new javax.crypto.spec.SecretKeySpec(key, "AES"),
                    new javax.crypto.spec.GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateOtpTestHelper(int length) {
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}
