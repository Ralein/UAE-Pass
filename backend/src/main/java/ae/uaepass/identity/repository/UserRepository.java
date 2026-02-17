package ae.uaepass.identity.repository;

import ae.uaepass.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmiratesIdHash(String emiratesIdHash);
    Optional<User> findByEmailHash(String emailHash);
    Optional<User> findByPhoneHash(String phoneHash);
    boolean existsByEmiratesIdHash(String emiratesIdHash);
}
