package ae.uaepass.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Type-safe configuration properties for the identity platform.
 * All secrets loaded from environment variables — never hardcoded.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
    CorsProperties cors,
    JwtProperties jwt,
    CryptoProperties crypto,
    OtpProperties otp,
    RateLimitProperties rateLimit
) {
    public record CorsProperties(
        @NotBlank String allowedOrigins
    ) {}

    public record JwtProperties(
        @NotBlank String keystorePath,
        @NotBlank String keystorePassword,
        @NotBlank String keyAlias,
        @NotBlank String keyPassword,
        @Positive int accessTokenTtlSeconds,
        @Positive int refreshTokenTtlSeconds
    ) {}

    public record CryptoProperties(
        @NotBlank String serverPepper,
        @NotBlank String aesKey,
        @NotBlank String hashSalt
    ) {}

    public record OtpProperties(
        @Positive int length,
        @Positive int expirySeconds,
        @Positive int maxAttempts,
        @Positive int resendCooldownSeconds,
        @Positive int maxCyclesBeforeLock
    ) {}

    public record RateLimitProperties(
        @Positive int authorizePerMinute,
        @Positive int tokenPerMinute,
        @Positive int otpPerMinute,
        @Positive int registrationPerMinute
    ) {}
}
