package ae.uaepass.identity.security;

import ae.uaepass.identity.config.AppSecurityProperties;
import org.springframework.stereotype.Component;

/**
 * Default secrets provider that reads from environment variables
 * via Spring's property binding.
 *
 * For production Vault/KMS integration, create an alternative
 * implementation and mark it @Primary.
 */
@Component
public class EnvironmentSecretsProvider implements SecretsProvider {

    private final AppSecurityProperties properties;

    public EnvironmentSecretsProvider(AppSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getSecret(String key) {
        return switch (key) {
            case "encryption-key" -> getEncryptionKey();
            case "server-pepper" -> getServerPepper();
            case "audit-hmac-key" -> getAuditHmacKey();
            default -> System.getenv(key);
        };
    }

    @Override
    public String getEncryptionKey() {
        return properties.crypto().aesKey();
    }

    @Override
    public String getServerPepper() {
        return properties.crypto().serverPepper();
    }

    @Override
    public String getAuditHmacKey() {
        // Use server pepper as HMAC key if no dedicated key configured
        String hmacKey = System.getenv("AUDIT_HMAC_KEY");
        return hmacKey != null ? hmacKey : properties.crypto().serverPepper();
    }
}
