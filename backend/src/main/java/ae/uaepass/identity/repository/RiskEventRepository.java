package ae.uaepass.identity.repository;

import ae.uaepass.identity.entity.RiskEvent;
import ae.uaepass.identity.entity.RiskEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RiskEventRepository extends JpaRepository<RiskEvent, UUID> {

    List<RiskEvent> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<RiskEvent> findByResolvedFalseOrderByCreatedAtDesc();

    @Query("SELECT COUNT(re) FROM RiskEvent re WHERE re.user.id = :userId " +
           "AND re.eventType = :eventType AND re.createdAt > :since")
    long countRecentByUserAndType(
        @Param("userId") UUID userId,
        @Param("eventType") RiskEventType eventType,
        @Param("since") Instant since
    );

    @Query("SELECT COALESCE(AVG(re.riskScore), 0) FROM RiskEvent re WHERE re.user.id = :userId " +
           "AND re.createdAt > :since AND re.resolved = false")
    double averageUnresolvedRiskScore(
        @Param("userId") UUID userId,
        @Param("since") Instant since
    );
}
