package ae.uaepass.identity.repository;

import ae.uaepass.identity.entity.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, UUID> {

    List<DeviceSession> findByUserIdAndActiveTrue(UUID userId);

    @Query("SELECT ds FROM DeviceSession ds WHERE ds.user.id = :userId " +
           "AND ds.deviceFingerprintHash = :fingerprint AND ds.active = true")
    Optional<DeviceSession> findActiveByUserAndFingerprint(
        @Param("userId") UUID userId,
        @Param("fingerprint") String fingerprintHash
    );

    @Query("SELECT COUNT(ds) FROM DeviceSession ds WHERE ds.user.id = :userId AND ds.active = true")
    long countActiveByUserId(@Param("userId") UUID userId);
}
