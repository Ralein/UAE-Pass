package ae.uaepass.identity.controller;

import ae.uaepass.identity.dto.RegistrationStartRequest;
import ae.uaepass.identity.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Registration endpoints.
 * No business logic here — all delegated to RegistrationService.
 */
@RestController
@RequestMapping("/api/v1/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Start a new registration.
     * Validates identity info, creates user record, sends OTP.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startRegistration(
            @Valid @RequestBody RegistrationStartRequest request,
            HttpServletRequest httpRequest) {

        UUID userId = registrationService.startRegistration(request, httpRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "userId", userId,
            "status", "OTP_SENT",
            "message", "Registration started. OTP sent to your phone."
        ));
    }

    /**
     * Get registration status for a user.
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable UUID userId) {
        var status = registrationService.getRegistrationStatus(userId);
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "status", status.name()
        ));
    }
}
