package ae.uaepass.identity.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.UUID;

/**
 * Injects a unique request ID into MDC for structured audit logging.
 * The request ID is propagated via X-Request-ID header or auto-generated.
 */
@Configuration
public class RequestTraceConfig {

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    private static class RequestIdFilter implements Filter {
        private static final String REQUEST_ID_HEADER = "X-Request-ID";
        private static final String MDC_KEY = "requestId";

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            try {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
                if (requestId == null || requestId.isBlank()) {
                    requestId = UUID.randomUUID().toString();
                }
                MDC.put(MDC_KEY, requestId);
                request.setAttribute(MDC_KEY, requestId);
                chain.doFilter(request, response);
            } finally {
                MDC.remove(MDC_KEY);
            }
        }
    }
}
