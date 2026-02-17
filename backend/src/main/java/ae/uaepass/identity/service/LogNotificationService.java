package ae.uaepass.identity.service;

import ae.uaepass.identity.util.PiiMaskingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Development-only notification service.
 *
 * SECURITY:
 * - OTPs are logged to a SEPARATE logger (identity.otp.delivery)
 * - This logger should output to a dedicated, access-controlled file
 * - OTP values are NEVER logged to the main application log
 * - Phone numbers are masked in all log output
 *
 * Configure logback.xml:
 *   <logger name="identity.otp.delivery" level="INFO">
 *     <appender-ref ref="OTP_FILE" />
 *   </logger>
 */
@Service
@Profile("dev")
public class LogNotificationService implements NotificationService {

    // Separate logger for OTP delivery — should go to secured file only
    private static final Logger otpLogger = LoggerFactory.getLogger("identity.otp.delivery");
    private static final Logger log = LoggerFactory.getLogger(LogNotificationService.class);

    @Override
    public void sendSms(String phoneNumber, String message) {
        // Log OTP to secured delivery log ONLY — never to main log
        otpLogger.info("[DEV-SMS] to={} body={}", PiiMaskingUtil.maskPhone(phoneNumber), message);
        log.info("SMS sent to {} (dev mode — check OTP delivery log)", PiiMaskingUtil.maskPhone(phoneNumber));
    }

    @Override
    public void sendEmail(String emailAddress, String subject, String body) {
        otpLogger.info("[DEV-EMAIL] to={} subject={} body={}", PiiMaskingUtil.maskEmail(emailAddress), subject, body);
        log.info("Email sent to {} (dev mode — check OTP delivery log)", PiiMaskingUtil.maskEmail(emailAddress));
    }
}
