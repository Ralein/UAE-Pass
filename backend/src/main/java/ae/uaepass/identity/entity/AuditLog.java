package ae.uaepass.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_fingerprint_hash", length = 128)
    private String deviceFingerprintHash;

    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- Builder-style setters for fluent construction ---

    public AuditLog eventType(AuditEventType eventType) {
        this.eventType = eventType;
        return this;
    }

    public AuditLog userId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public AuditLog requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public AuditLog ipAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public AuditLog deviceFingerprintHash(String hash) {
        this.deviceFingerprintHash = hash;
        return this;
    }

    public AuditLog details(String details) {
        this.details = details;
        return this;
    }

    public Long getId() { return id; }
    public AuditEventType getEventType() { return eventType; }
    public UUID getUserId() { return userId; }
    public String getRequestId() { return requestId; }
    public String getIpAddress() { return ipAddress; }
    public String getDeviceFingerprintHash() { return deviceFingerprintHash; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
