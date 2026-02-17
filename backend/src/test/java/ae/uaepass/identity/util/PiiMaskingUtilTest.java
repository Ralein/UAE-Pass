package ae.uaepass.identity.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PII masking utility.
 * Ensures masking is irreversible and follows expected patterns.
 */
class PiiMaskingUtilTest {

    @Test
    void maskEmail_standardEmail_masksLocalPart() {
        assertEquals("j***@example.com", PiiMaskingUtil.maskEmail("john@example.com"));
    }

    @Test
    void maskEmail_singleCharLocal_masks() {
        assertEquals("***@x.com", PiiMaskingUtil.maskEmail("a@x.com"));
    }

    @Test
    void maskEmail_null_returnsStars() {
        assertEquals("***", PiiMaskingUtil.maskEmail(null));
    }

    @Test
    void maskPhone_uaeNumber_masksMiddle() {
        assertEquals("+971****5678", PiiMaskingUtil.maskPhone("+97150125678"));
    }

    @Test
    void maskPhone_null_returnsStars() {
        assertEquals("***", PiiMaskingUtil.maskPhone(null));
    }

    @Test
    void maskEmiratesId_validFormat() {
        String masked = PiiMaskingUtil.maskEmiratesId("784-1990-1234567-0");
        assertEquals("784-****-*******-0", masked);
    }

    @Test
    void maskName_fullName_masksLastName() {
        assertEquals("John D***", PiiMaskingUtil.maskName("John Doe"));
    }

    @Test
    void maskName_singleName_masksAfterFirst() {
        assertEquals("J***", PiiMaskingUtil.maskName("John"));
    }

    @Test
    void maskUuid_standard_showsFirst8() {
        String masked = PiiMaskingUtil.maskUuid("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        assertEquals("a1b2c3d4-****", masked);
    }

    @Test
    void maskIp_ipv4_masksLastTwoOctets() {
        assertEquals("192.168.*.*", PiiMaskingUtil.maskIp("192.168.1.100"));
    }

    @Test
    void mask_generic_showsNChars() {
        assertEquals("abc***", PiiMaskingUtil.mask("abcdefgh", 3));
    }
}
