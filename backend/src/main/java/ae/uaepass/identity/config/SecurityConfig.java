package ae.uaepass.identity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Core Spring Security configuration.
 * Security decisions documented inline for audit review.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {

    private final AppSecurityProperties securityProps;

    public SecurityConfig(AppSecurityProperties securityProps) {
        this.securityProps = securityProps;
    }

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // --- CORS: strict allowlist, no wildcards ---
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // --- CSRF: enabled for browser sessions using double-submit cookie ---
            // The cookie is NOT HttpOnly so the frontend JS can read it for the X-XSRF-TOKEN header.
            // The actual session cookie IS HttpOnly (configured in application.yml).
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    "/oauth2/token",      // Token endpoint uses client credentials, not cookies
                    "/oauth2/introspect", // Machine-to-machine
                    "/oauth2/revoke"      // Machine-to-machine
                )
            )

            // --- Security Headers ---
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)  // 1 year
                )
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(content -> {})
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .permissionsPolicy(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=(), payment=()")
                )
            )

            // --- Session Management ---
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fix -> fix.migrateSession())
                .maximumSessions(3)
            )

            // --- Authorization Rules ---
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/v1/registration/**",
                    "/api/v1/otp/**",
                    "/oauth2/authorize",
                    "/oauth2/token",
                    "/oauth2/.well-known/**",
                    "/.well-known/openid-configuration",
                    "/oauth2/jwks",
                    "/actuator/health"
                ).permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * CORS: Only the configured frontend origin is allowed.
     * No wildcard origins. Credentials allowed for cookie-based sessions.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(securityProps.cors().allowedOrigins()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "X-XSRF-TOKEN",
            "X-Request-ID", "X-Device-Fingerprint"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * BCrypt encoder for Spring Security's internal use (e.g., client secrets).
     * PIN hashing uses Argon2id via CryptoService — not this bean.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
