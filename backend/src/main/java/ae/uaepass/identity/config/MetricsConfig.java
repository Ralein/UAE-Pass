package ae.uaepass.identity.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer metrics for security observability.
 *
 * Counters:
 * - identity.auth.success / identity.auth.failure
 * - identity.otp.sent / identity.otp.failure / identity.otp.verified
 * - identity.token.issued / identity.token.revoked / identity.token.replayed
 * - identity.lockout.count
 * - identity.registration.started / identity.registration.completed
 *
 * Timers:
 * - identity.auth.flow.latency
 *
 * All exposed via /actuator/prometheus
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter authSuccessCounter(MeterRegistry registry) {
        return Counter.builder("identity.auth.success")
            .description("Successful authentications")
            .register(registry);
    }

    @Bean
    public Counter authFailureCounter(MeterRegistry registry) {
        return Counter.builder("identity.auth.failure")
            .description("Failed authentications")
            .register(registry);
    }

    @Bean
    public Counter otpSentCounter(MeterRegistry registry) {
        return Counter.builder("identity.otp.sent")
            .description("OTPs sent")
            .register(registry);
    }

    @Bean
    public Counter otpFailureCounter(MeterRegistry registry) {
        return Counter.builder("identity.otp.failure")
            .description("Failed OTP verifications")
            .register(registry);
    }

    @Bean
    public Counter otpVerifiedCounter(MeterRegistry registry) {
        return Counter.builder("identity.otp.verified")
            .description("Successful OTP verifications")
            .register(registry);
    }

    @Bean
    public Counter tokenIssuedCounter(MeterRegistry registry) {
        return Counter.builder("identity.token.issued")
            .description("Tokens issued")
            .register(registry);
    }

    @Bean
    public Counter tokenRevokedCounter(MeterRegistry registry) {
        return Counter.builder("identity.token.revoked")
            .description("Tokens revoked")
            .register(registry);
    }

    @Bean
    public Counter tokenReplayedCounter(MeterRegistry registry) {
        return Counter.builder("identity.token.replayed")
            .description("Token replay attempts detected")
            .register(registry);
    }

    @Bean
    public Counter lockoutCounter(MeterRegistry registry) {
        return Counter.builder("identity.lockout.count")
            .description("Account lockouts triggered")
            .register(registry);
    }

    @Bean
    public Counter registrationStartedCounter(MeterRegistry registry) {
        return Counter.builder("identity.registration.started")
            .description("Registration flows started")
            .register(registry);
    }

    @Bean
    public Counter registrationCompletedCounter(MeterRegistry registry) {
        return Counter.builder("identity.registration.completed")
            .description("Registration flows completed")
            .register(registry);
    }

    @Bean
    public Timer authFlowLatencyTimer(MeterRegistry registry) {
        return Timer.builder("identity.auth.flow.latency")
            .description("Authentication flow end-to-end latency")
            .register(registry);
    }
}
