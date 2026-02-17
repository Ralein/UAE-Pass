package ae.uaepass.identity.security;

import ae.uaepass.identity.entity.AuditEventType;
import ae.uaepass.identity.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.Map;

/**
 * JWT key rotation scheduler.
 *
 * Checks key validity windows daily. When a key approaches expiry
 * (within the configured warning threshold), it logs a warning and
 * an audit event so operators can rotate keys proactively.
 *
 * In a Vault/KMS setup, this would trigger automatic key generation.
 * With file-based keystores, it alerts operators to perform manual rotation.
 *
 * Multiple key aliases are supported:
 * - The newest valid key is used for signing
 * - All valid keys are published in the JWK set for verification
 */
@Component
public class JwkRotationScheduler {

    private static final Logger log = LoggerFactory.getLogger(JwkRotationScheduler.class);

    private final AuditService auditService;
    private final SecretsProvider secretsProvider;

    @Value("${app.security.jwt.keystore-path}")
    private String keystorePath;

    @Value("${app.security.jwt.keystore-password}")
    private String keystorePassword;

    @Value("${app.security.jwt.key-validity-days:365}")
    private int keyValidityDays;

    @Value("${app.security.jwt.rotation-warning-days:30}")
    private int rotationWarningDays;

    public JwkRotationScheduler(AuditService auditService, SecretsProvider secretsProvider) {
        this.auditService = auditService;
        this.secretsProvider = secretsProvider;
    }

    /**
     * Check key validity daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkKeyValidity() {
        log.info("Running JWT key validity check");

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (var is = new java.io.FileInputStream(keystorePath)) {
                keyStore.load(is, keystorePassword.toCharArray());
            }

            Enumeration<String> aliases = keyStore.aliases();
            Instant now = Instant.now();
            Instant warningThreshold = now.plus(rotationWarningDays, ChronoUnit.DAYS);

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                    if (cert != null) {
                        Instant expiry = cert.getNotAfter().toInstant();

                        if (expiry.isBefore(now)) {
                            log.error("JWT key '{}' has EXPIRED at {}. Immediate rotation required!", alias, expiry);
                            auditService.logEvent(AuditEventType.KEY_ROTATION_NEEDED, null, null,
                                Map.of("alias", alias, "status", "EXPIRED", "expiry", expiry.toString()));
                        } else if (expiry.isBefore(warningThreshold)) {
                            long daysUntilExpiry = ChronoUnit.DAYS.between(now, expiry);
                            log.warn("JWT key '{}' expires in {} days ({}). Rotation recommended.",
                                alias, daysUntilExpiry, expiry);
                            auditService.logEvent(AuditEventType.KEY_ROTATION_NEEDED, null, null,
                                Map.of("alias", alias, "status", "EXPIRING_SOON",
                                       "daysUntilExpiry", String.valueOf(daysUntilExpiry)));

                            // Notify secrets provider for auto-rotation (Vault/KMS)
                            secretsProvider.rotateKey(alias);
                        } else {
                            log.debug("JWT key '{}' valid until {}", alias, expiry);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to check JWT key validity", e);
        }
    }
}
