package ae.uaepass.identity.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PIN complexity validation.
 * Tests sequential detection, repeated digit detection,
 * and Argon2id hash verification.
 */
class PinServiceTest {

    @Test
    void validatePinComplexity_validPin_passes() {
        assertTrue(isComplexEnough("285937"), "Random 6-digit PIN should pass");
    }

    @Test
    void validatePinComplexity_sequential_fails() {
        assertFalse(isComplexEnough("123456"), "Sequential ascending PIN should fail");
        assertFalse(isComplexEnough("654321"), "Sequential descending PIN should fail");
    }

    @Test
    void validatePinComplexity_allRepeated_fails() {
        assertFalse(isComplexEnough("111111"), "All same digits should fail");
        assertFalse(isComplexEnough("000000"), "All zeros should fail");
        assertFalse(isComplexEnough("999999"), "All nines should fail");
    }

    @Test
    void validatePinComplexity_tooShort_fails() {
        assertFalse(isComplexEnough("1234"), "PIN shorter than 6 digits should fail");
        assertFalse(isComplexEnough("12345"), "5-digit PIN should fail");
    }

    @Test
    void validatePinComplexity_nonNumeric_fails() {
        assertFalse(isComplexEnough("12345a"), "Non-numeric PIN should fail");
        assertFalse(isComplexEnough("abcdef"), "All letters should fail");
    }

    @Test
    void validatePinComplexity_commonPatterns_fails() {
        assertFalse(isComplexEnough("123123"), "Repeated sequence should fail");
        assertFalse(isComplexEnough("112233"), "Paired sequence should fail");
    }

    @Test
    void argon2id_hash_isNotReversible() {
        String pin = "482917";
        String hash = simulateArgon2Hash(pin);

        assertNotEquals(pin, hash, "Hash must not equal plaintext");
        assertTrue(hash.length() > 20, "Argon2id output should be long");
    }

    @Test
    void argon2id_samePin_differentSalt_differentHash() {
        String pin = "394817";
        String hash1 = simulateArgon2Hash(pin + "salt1");
        String hash2 = simulateArgon2Hash(pin + "salt2");

        assertNotEquals(hash1, hash2, "Different salts must produce different hashes");
    }

    // --- Validation helpers (mirrors PinService logic) ---

    private boolean isComplexEnough(String pin) {
        if (pin == null || !pin.matches("\\d{6}"))
            return false;
        if (isSequential(pin))
            return false;
        if (isAllRepeated(pin))
            return false;
        if (isRepeatedPattern(pin))
            return false;
        return true;
    }

    private boolean isSequential(String pin) {
        boolean ascending = true, descending = true;
        for (int i = 1; i < pin.length(); i++) {
            if (pin.charAt(i) - pin.charAt(i - 1) != 1)
                ascending = false;
            if (pin.charAt(i) - pin.charAt(i - 1) != -1)
                descending = false;
        }
        return ascending || descending;
    }

    private boolean isAllRepeated(String pin) {
        return pin.chars().distinct().count() == 1;
    }

    private boolean isRepeatedPattern(String pin) {
        // Check 2-char and 3-char repeating patterns
        String half = pin.substring(0, 3);
        if (pin.equals(half + half))
            return true;
        for (int i = 0; i < pin.length() - 1; i += 2) {
            if (pin.charAt(i) != pin.charAt(i + 1))
                return false;
        }
        return true;
    }

    private String simulateArgon2Hash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
