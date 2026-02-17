package ae.uaepass.identity.repository;

import ae.uaepass.identity.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, UUID> {
    Optional<Consent> findByUserIdAndClientId(UUID userId, String clientId);
}
