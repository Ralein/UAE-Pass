package ae.uaepass.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "emirates_id_hash", nullable = false, unique = true, length = 128)
    private String emiratesIdHash;

    @Column(name = "email_hash", nullable = false, length = 128)
    private String emailHash;

    @Column(name = "phone_hash", nullable = false, length = 128)
    private String phoneHash;

    @Column(name = "full_name_enc")
    private String fullNameEnc;

    @Column(length = 10)
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_level", nullable = false, length = 10)
    private AccountLevel accountLevel = AccountLevel.SOP1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmiratesIdHash() { return emiratesIdHash; }
    public void setEmiratesIdHash(String emiratesIdHash) { this.emiratesIdHash = emiratesIdHash; }

    public String getEmailHash() { return emailHash; }
    public void setEmailHash(String emailHash) { this.emailHash = emailHash; }

    public String getPhoneHash() { return phoneHash; }
    public void setPhoneHash(String phoneHash) { this.phoneHash = phoneHash; }

    public String getFullNameEnc() { return fullNameEnc; }
    public void setFullNameEnc(String fullNameEnc) { this.fullNameEnc = fullNameEnc; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public AccountLevel getAccountLevel() { return accountLevel; }
    public void setAccountLevel(AccountLevel accountLevel) { this.accountLevel = accountLevel; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
