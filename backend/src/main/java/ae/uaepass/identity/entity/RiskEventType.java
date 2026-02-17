package ae.uaepass.identity.entity;

public enum RiskEventType {
    BRUTE_FORCE,
    NEW_DEVICE,
    UNUSUAL_IP,
    VELOCITY_ANOMALY,
    SESSION_HIJACK,
    TOKEN_REPLAY,
    OTP_ABUSE,
    PIN_LOCKOUT,
    ACCOUNT_TAKEOVER_ATTEMPT,
    GEO_ANOMALY
}
