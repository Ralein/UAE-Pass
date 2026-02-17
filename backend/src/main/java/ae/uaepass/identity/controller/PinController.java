package ae.uaepass.identity.controller;

import ae.uaepass.identity.dto.PinCreateRequest;
import ae.uaepass.identity.service.PinService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * PIN creation endpoint.
 * Only accessible after OTP verification.
 */
@RestController
@RequestMapping("/api/v1/pin")
public class PinController {

    private final PinService pinService;

    public PinController(PinService pinService) {
        this.pinService = pinService;
    }

    /**
     * Create PIN for a verified user.
     * PIN is validated for complexity and hashed with Argon2id before storage.
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPin(
            @Valid @RequestBody PinCreateRequest request,
            HttpServletRequest httpRequest) {

        // Confirm PIN matches
        if (!request.pin().equals(request.pinConfirm())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "pin_mismatch",
                "message", "PIN and confirmation do not match"
            ));
        }

        pinService.createPin(request.userId(), request.pin(), httpRequest);

        return ResponseEntity.ok(Map.of(
            "status", "ACTIVE",
            "message", "PIN created successfully. Account is now active."
        ));
    }
}
