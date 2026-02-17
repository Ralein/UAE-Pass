package ae.uaepass.identity.util;

/**
 * PII masking utility for audit logs and error responses.
 *
 * SECURITY: These methods produce irreversible masked output.
 * Use for logging and display only — never for storage (use hash/encrypt for that).
 */
public final class PiiMaskingUtil {

    private PiiMaskingUtil() {} // Utility class

    /**
     * Mask email: j***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIdx = email.indexOf("@");
        if (atIdx <= 1) return "***" + email.substring(atIdx);
        return email.charAt(0) + "***" + email.substring(atIdx);
    }

    /**
     * Mask phone: +971****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * Mask Emirates ID: 784-****-*******-0
     */
    public static String maskEmiratesId(String eid) {
        if (eid == null || eid.length() < 5) return "***";
        return eid.substring(0, 4) + "****-*******-" + eid.charAt(eid.length() - 1);
    }

    /**
     * Mask name: John D***
     */
    public static String maskName(String name) {
        if (name == null || name.length() < 2) return "***";
        String[] parts = name.split("\\s+");
        if (parts.length == 1) return name.charAt(0) + "***";
        return parts[0] + " " + parts[1].charAt(0) + "***";
    }

    /**
     * Mask UUID: show first 8 chars only.
     */
    public static String maskUuid(String uuid) {
        if (uuid == null || uuid.length() < 8) return "***";
        return uuid.substring(0, 8) + "-****";
    }

    /**
     * Mask IP address: keep first two octets.
     */
    public static String maskIp(String ip) {
        if (ip == null) return "***";
        String[] parts = ip.split("\\.");
        if (parts.length < 4) return ip; // IPv6 or unexpected
        return parts[0] + "." + parts[1] + ".*.*";
    }

    /**
     * Generic mask: show first N chars, mask the rest.
     */
    public static String mask(String value, int visibleChars) {
        if (value == null) return "***";
        if (value.length() <= visibleChars) return value;
        return value.substring(0, visibleChars) + "***";
    }
}
