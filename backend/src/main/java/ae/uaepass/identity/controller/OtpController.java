package ae.uaepass.identity.controller;

import ae.uaepass.identity.dto.OtpSendRequest;
import ae.uaepass.identity.dto.OtpVerifyRequest;
import ae.uaepass.identity.entity.OtpChannel;
import ae.uaepass.identity.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OTP send and verify endpoints.
 * Rate-limited at the filter layer (5 requests/minute per IP).
 */
@RestController
@RequestMapping("/api/v1/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Send or resend OTP.
     * The actual OTP is delivered via notification service (SMS/Email).
     * Response only confirms that OTP was sent — never returns the OTP value.
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @Valid @RequestBody OtpSendRequest request,
            HttpServletRequest httpRequest) {

        OtpChannel channel = OtpChannel.valueOf(request.channel());

        // Generate OTP — returned value is for notification service only
        String otp = otpService.generateOtp(request.userId(), channel, httpRequest);

        // TODO: Integrate with SMS/Email notification service
        // For development: OTP is logged to a separate secured audit file
        // In production: otp would be sent via Twilio/SNS and never returned here

        return ResponseEntity.ok(Map.of(
            "status", "OTP_SENT",
            "channel", channel.name(),
            "message", "OTP sent to your " + (channel == OtpChannel.SMS ? "phone" : "email"),
            "expiresInSeconds", 180,
            "cooldownSeconds", 60
        ));
    }

    /**
     * Verify an OTP code.
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            HttpServletRequest httpRequest) {

        OtpChannel channel = OtpChannel.valueOf(request.channel());
        boolean verified = otpService.verifyOtp(
            request.userId(), channel, request.otpCode(), httpRequest
        );

        if (verified) {
            return ResponseEntity.ok(Map.of(
                "status", "OTP_VERIFIED",
                "message", "OTP verified successfully"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "OTP_INVALID",
                "message", "Invalid OTP. Please try again."
            ));
        }
    }
}
