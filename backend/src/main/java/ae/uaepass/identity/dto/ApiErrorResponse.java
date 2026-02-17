package ae.uaepass.identity.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized API error response.
 * Consistent error format across all endpoints.
 */
public record ApiErrorResponse(
    String error,
    String message,
    String requestId,
    Instant timestamp,
    Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(String error, String message, String requestId) {
        return new ApiErrorResponse(error, message, requestId, Instant.now(), null);
    }

    public static ApiErrorResponse withFieldErrors(String error, String message, String requestId,
                                                    Map<String, String> fieldErrors) {
        return new ApiErrorResponse(error, message, requestId, Instant.now(), fieldErrors);
    }
}
