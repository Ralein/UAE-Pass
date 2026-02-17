package ae.uaepass.identity.controller;

import ae.uaepass.identity.entity.User;
import ae.uaepass.identity.repository.UserRepository;
import ae.uaepass.identity.service.CryptoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Authenticated user profile endpoint.
 * Decrypts stored PII fields for the requesting user only.
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserInfoController {

    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    public UserInfoController(UserRepository userRepository, CryptoService cryptoService) {
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
    }

    /**
     * Get current authenticated user's profile.
     * Requires valid JWT.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {

        String userIdStr = jwt.getSubject();
        UUID userId = UUID.fromString(userIdStr);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Decrypt PII for display
        String fullName = user.getFullNameEnc() != null
            ? cryptoService.decrypt(user.getFullNameEnc()) : null;

        return ResponseEntity.ok(Map.of(
            "userId", user.getId(),
            "fullName", fullName != null ? fullName : "",
            "gender", user.getGender() != null ? user.getGender() : "",
            "status", user.getStatus().name(),
            "accountLevel", user.getAccountLevel().name(),
            "createdAt", user.getCreatedAt().toString()
        ));
    }
}
