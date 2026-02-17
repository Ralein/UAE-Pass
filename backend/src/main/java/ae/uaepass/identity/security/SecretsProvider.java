package ae.uaepass.identity.security;

/**
 * Abstraction for secrets management.
 *
 * Default implementation reads from environment variables.
 * Production can swap in Vault/KMS implementation without code changes —
 * just provide a different @Bean.
 */
public interface SecretsProvider {

    /**
     * Retrieve a secret by key identifier.
     */
    String getSecret(String key);

    /**
     * Get the AES-256 encryption key for PII fields.
     */
    String getEncryptionKey();

    /**
     * Get the server pepper for PIN hashing.
     */
    String getServerPepper();

    /**
     * Get the HMAC key for audit log tamper detection.
     */
    String getAuditHmacKey();

    /**
     * Notify the provider that a key should be rotated.
     * No-op for environment-based provider.
     */
    default void rotateKey(String keyId) {
        // Default: no-op. Vault/KMS implementations override this.
    }
}
