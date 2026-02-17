package ae.uaepass.identity.entity;

public enum AccountLevel {
    SOP1,  // Mobile + Email verified, Emirates ID not verified
    SOP2,  // Mobile + Email + Emirates ID verified
    SOP3   // Full verification with biometrics
}
