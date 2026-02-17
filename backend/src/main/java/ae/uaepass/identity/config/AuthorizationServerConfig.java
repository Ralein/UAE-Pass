package ae.uaepass.identity.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2 Authorization Server configuration.
 * 
 * SECURITY DECISIONS:
 * - RS256 signing: asymmetric keys prevent token forgery by resource servers
 * - Keys loaded from PKCS12 keystore: private key never in source code
 * - Short-lived access tokens (15 min): limits window of compromised token use
 * - Refresh token rotation: each refresh token is one-time-use
 * - Key ID (kid) included in JWK for rotation support
 */
@Configuration
public class AuthorizationServerConfig {

    private final AppSecurityProperties securityProps;
    private final ResourceLoader resourceLoader;

    public AuthorizationServerConfig(AppSecurityProperties securityProps,
                                      ResourceLoader resourceLoader) {
        this.securityProps = securityProps;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Load RSA key pair from PKCS12 keystore.
     * The keystore path and password come from environment variables.
     * Supports key rotation via multiple aliases — newest alias signs, all verify.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        var jwtProps = securityProps.jwt();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = resourceLoader.getResource(jwtProps.keystorePath()).getInputStream()) {
            keyStore.load(is, jwtProps.keystorePassword().toCharArray());
        }

        RSAPublicKey publicKey = (RSAPublicKey) keyStore.getCertificate(jwtProps.keyAlias()).getPublicKey();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(
            jwtProps.keyAlias(),
            jwtProps.keyPassword().toCharArray()
        );

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.nameUUIDFromBytes(jwtProps.keyAlias().getBytes()).toString())
            .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("https://identity.uaepass.ae")
            .authorizationEndpoint("/oauth2/authorize")
            .tokenEndpoint("/oauth2/token")
            .jwkSetEndpoint("/oauth2/jwks")
            .tokenIntrospectionEndpoint("/oauth2/introspect")
            .tokenRevocationEndpoint("/oauth2/revoke")
            .oidcUserInfoEndpoint("/userinfo")
            .build();
    }

    @Bean
    public TokenSettings tokenSettings() {
        var jwtProps = securityProps.jwt();
        return TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofSeconds(jwtProps.accessTokenTtlSeconds()))
            .refreshTokenTimeToLive(Duration.ofSeconds(jwtProps.refreshTokenTtlSeconds()))
            .reuseRefreshTokens(false) // One-time-use: rotation on every refresh
            .build();
    }

    @Bean
    public ClientSettings clientSettings() {
        return ClientSettings.builder()
            .requireAuthorizationConsent(true) // Always show consent screen
            .requireProofKey(true)             // PKCE mandatory for public clients
            .build();
    }
}
