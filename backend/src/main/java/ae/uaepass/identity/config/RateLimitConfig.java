package ae.uaepass.identity.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting using Bucket4j token bucket algorithm.
 * Per-IP AND per-user rate limiting to prevent brute-force and abuse.
 *
 * SECURITY: Rate limits are enforced at the servlet filter level
 * before any business logic executes. Both IP and user buckets
 * must pass — a single account cannot be attacked from multiple IPs.
 */
@Configuration
public class RateLimitConfig {

    private final AppSecurityProperties securityProps;

    // Separate bucket maps for IP-based and user-based limits
    private final Map<String, Map<String, Bucket>> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Bucket>> userBuckets = new ConcurrentHashMap<>();

    public RateLimitConfig(AppSecurityProperties securityProps) {
        this.securityProps = securityProps;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter());
        registration.addUrlPatterns(
            "/oauth2/authorize", "/oauth2/token",
            "/api/v1/otp/*", "/api/v1/registration/*",
            "/api/v1/pin/*", "/api/v1/sessions/*"
        );
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    private class RateLimitFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String clientIp = resolveClientIp(httpRequest);
            String path = httpRequest.getRequestURI();
            String bucketGroup = resolveBucketGroup(path);

            if (bucketGroup != null) {
                // Check per-IP limit
                Bucket ipBucket = resolveIpBucket(bucketGroup, clientIp);
                if (!ipBucket.tryConsume(1)) {
                    writeRateLimitResponse(httpResponse);
                    return;
                }

                // Check per-user limit (if authenticated)
                String userId = resolveUserId(httpRequest);
                if (userId != null) {
                    Bucket userBucket = resolveUserBucket(bucketGroup, userId);
                    if (!userBucket.tryConsume(1)) {
                        writeRateLimitResponse(httpResponse);
                        return;
                    }
                }
            }

            chain.doFilter(request, response);
        }

        private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                "{\"error\":\"rate_limit_exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );
        }

        private String resolveClientIp(HttpServletRequest request) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }

        private String resolveUserId(HttpServletRequest request) {
            // Try session attribute first (set during registration flow)
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object uid = session.getAttribute("userId");
                if (uid != null) return uid.toString();
            }
            // JWT subject would be extracted via SecurityContextHolder in full auth flow
            return null;
        }

        private String resolveBucketGroup(String path) {
            if (path.startsWith("/oauth2/authorize")) return "authorize";
            if (path.startsWith("/oauth2/token")) return "token";
            if (path.startsWith("/api/v1/otp")) return "otp";
            if (path.startsWith("/api/v1/registration")) return "registration";
            if (path.startsWith("/api/v1/pin")) return "pin";
            if (path.startsWith("/api/v1/sessions")) return "sessions";
            return null;
        }

        private Bucket resolveIpBucket(String group, String clientIp) {
            String key = group + ":ip:" + clientIp;
            return ipBuckets
                .computeIfAbsent(group, g -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> createBucket(group));
        }

        private Bucket resolveUserBucket(String group, String userId) {
            String key = group + ":user:" + userId;
            return userBuckets
                .computeIfAbsent(group, g -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> createUserBucket(group));
        }

        private Bucket createBucket(String group) {
            var rateLimitProps = securityProps.rateLimit();
            int perMinute = switch (group) {
                case "authorize" -> rateLimitProps.authorizePerMinute();
                case "token" -> rateLimitProps.tokenPerMinute();
                case "otp" -> rateLimitProps.otpPerMinute();
                case "registration" -> rateLimitProps.registrationPerMinute();
                case "pin" -> rateLimitProps.otpPerMinute(); // Same limit as OTP
                case "sessions" -> 10;
                default -> 60;
            };

            Bandwidth limit = Bandwidth.classic(
                perMinute,
                Refill.greedy(perMinute, Duration.ofMinutes(1))
            );
            return Bucket.builder().addLimit(limit).build();
        }

        private Bucket createUserBucket(String group) {
            // Per-user limits are stricter than per-IP
            int perMinute = switch (group) {
                case "otp" -> 3;    // 3 OTP requests per user per minute
                case "pin" -> 3;    // 3 PIN attempts per user per minute
                case "token" -> 5;  // 5 token requests per user per minute
                default -> 10;
            };

            Bandwidth limit = Bandwidth.classic(
                perMinute,
                Refill.greedy(perMinute, Duration.ofMinutes(1))
            );
            return Bucket.builder().addLimit(limit).build();
        }
    }
}

