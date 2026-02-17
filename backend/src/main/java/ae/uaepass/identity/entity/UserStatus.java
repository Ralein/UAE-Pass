package ae.uaepass.identity.entity;

/**
 * User account status lifecycle.
 * State machine: PENDING → OTP_SENT → OTP_VERIFIED → ACTIVE
 * Can transition to LOCKED (too many failures) or SUSPENDED (admin action).
 */
public enum UserStatus {
    PENDING,
    OTP_SENT,
    OTP_VERIFIED,
    ACTIVE,
    LOCKED,
    SUSPENDED
}
