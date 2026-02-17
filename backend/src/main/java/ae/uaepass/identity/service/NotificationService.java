package ae.uaepass.identity.service;

/**
 * Notification service abstraction for OTP delivery.
 *
 * Implementations:
 * - LogNotificationService (dev) — logs OTP to secured file
 * - TwilioNotificationService (prod) — SMS via Twilio
 * - AwsSnsNotificationService (prod) — SMS via AWS SNS
 *
 * Swap implementations via Spring @Profile or @Primary.
 */
public interface NotificationService {

    /**
     * Send SMS message.
     * @param phoneNumber E.164 format (e.g., +971501234567)
     * @param message The message body
     * @throws NotificationException if delivery fails
     */
    void sendSms(String phoneNumber, String message);

    /**
     * Send email message.
     * @param emailAddress Recipient email
     * @param subject Email subject
     * @param body Email body (HTML supported)
     * @throws NotificationException if delivery fails
     */
    void sendEmail(String emailAddress, String subject, String body);

    /**
     * Check if the service is healthy/available.
     */
    default boolean isAvailable() {
        return true;
    }

    class NotificationException extends RuntimeException {
        public NotificationException(String message) { super(message); }
        public NotificationException(String message, Throwable cause) { super(message, cause); }
    }
}
