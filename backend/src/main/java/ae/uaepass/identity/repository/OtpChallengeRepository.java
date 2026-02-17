package ae.uaepass.identity.repository;

import ae.uaepass.identity.entity.OtpChallenge;
import ae.uaepass.identity.entity.OtpChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    @Query("SELECT o FROM OtpChallenge o WHERE o.user.id = :userId AND o.channel = :channel " +
           "AND o.consumed = false AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<OtpChallenge> findActiveByUserIdAndChannel(
        @Param("userId") UUID userId,
        @Param("channel") OtpChannel channel,
        @Param("now") Instant now
    );

    @Query("SELECT COUNT(o) FROM OtpChallenge o WHERE o.user.id = :userId " +
           "AND o.consumed = false AND o.createdAt > :since")
    long countRecentFailedCycles(
        @Param("userId") UUID userId,
        @Param("since") Instant since
    );

    @Query("SELECT o FROM OtpChallenge o WHERE o.user.id = :userId AND o.channel = :channel " +
           "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpChallenge> findLatestByUserIdAndChannel(
        @Param("userId") UUID userId,
        @Param("channel") OtpChannel channel
    );
}
