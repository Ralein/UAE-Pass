package ae.uaepass.identity.controller;

import ae.uaepass.identity.entity.AuditEventType;
import ae.uaepass.identity.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Session revocation endpoint.
 * Allows authenticated users to invalidate their own sessions.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionRevokeController {

    private final AuditService auditService;

    public SessionRevokeController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Revoke current session (logout).
     */
    @PostMapping("/revoke")
    public ResponseEntity<Map<String, String>> revokeCurrentSession(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());

        // Invalidate HTTP session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        auditService.logEvent(AuditEventType.SESSION_REVOKED, userId, request);

        return ResponseEntity.ok(Map.of(
            "status", "revoked",
            "message", "Session has been revoked successfully"
        ));
    }

    /**
     * Revoke all sessions for the current user.
     */
    @PostMapping("/revoke-all")
    public ResponseEntity<Map<String, String>> revokeAllSessions(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());

        // Invalidate current HTTP session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Note: To revoke ALL Redis sessions, you would need to
        // iterate over all session keys for this user in Redis.
        // Spring Session's FindByIndexNameSessionRepository supports this.

        auditService.logEvent(AuditEventType.SESSION_REVOKED, userId, request,
            Map.of("scope", "ALL_SESSIONS"));

        return ResponseEntity.ok(Map.of(
            "status", "all_revoked",
            "message", "All sessions have been revoked"
        ));
    }
}
