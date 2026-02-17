package ae.uaepass.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks known devices per user for trust scoring and session anomaly detection.
 */
@Entity
@Table(name = "device_sessions")
public class DeviceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_fingerprint_hash", nullable = false, length = 128)
    private String deviceFingerprintHash;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "trust_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TrustLevel trustLevel = TrustLevel.UNKNOWN;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "login_count", nullable = false)
    private int loginCount = 0;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.firstSeenAt = now;
        this.lastSeenAt = now;
    }

    // Getters & Setters

    public UUID getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDeviceFingerprintHash() { return deviceFingerprintHash; }
    public void setDeviceFingerprintHash(String hash) { this.deviceFingerprintHash = hash; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public Instant getFirstSeenAt() { return firstSeenAt; }

    public TrustLevel getTrustLevel() { return trustLevel; }
    public void setTrustLevel(TrustLevel trustLevel) { this.trustLevel = trustLevel; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getLoginCount() { return loginCount; }
    public void setLoginCount(int loginCount) { this.loginCount = loginCount; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
