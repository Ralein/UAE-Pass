package ae.uaepass.identity.controller;

import ae.uaepass.identity.entity.DeviceSession;
import ae.uaepass.identity.service.DeviceFingerprintService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Device management — view and revoke trusted devices.
 */
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceManagementController {

    private final DeviceFingerprintService deviceFingerprintService;

    public DeviceManagementController(DeviceFingerprintService deviceFingerprintService) {
        this.deviceFingerprintService = deviceFingerprintService;
    }

    /**
     * List all active (trusted) devices for the current user.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDevices(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        List<DeviceSession> devices = deviceFingerprintService.getActiveDevices(userId);

        List<Map<String, Object>> deviceList = devices.stream()
            .map(d -> Map.<String, Object>of(
                "id", d.getId(),
                "userAgent", d.getUserAgent() != null ? d.getUserAgent() : "Unknown",
                "trustLevel", d.getTrustLevel().name(),
                "lastSeen", d.getLastSeenAt().toString(),
                "firstSeen", d.getFirstSeenAt().toString(),
                "loginCount", d.getLoginCount()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "devices", deviceList,
            "totalDevices", deviceList.size()
        ));
    }

    /**
     * Revoke trust for a specific device.
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Map<String, String>> revokeDevice(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal Jwt jwt) {

        // Note: In production, verify that the device belongs to the requesting user
        deviceFingerprintService.revokeDevice(deviceId);

        return ResponseEntity.ok(Map.of(
            "status", "revoked",
            "message", "Device has been removed from trusted devices"
        ));
    }
}
