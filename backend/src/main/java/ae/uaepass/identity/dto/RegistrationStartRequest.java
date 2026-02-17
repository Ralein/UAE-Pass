package ae.uaepass.identity.dto;

import jakarta.validation.constraints.*;

/**
 * Registration start request DTO with full bean validation.
 * All validation is server-side — never trust frontend.
 */
public record RegistrationStartRequest(
    @NotBlank(message = "Emirates ID is required")
    @Pattern(
        regexp = "^784-\\d{4}-\\d{7}-\\d$",
        message = "Invalid Emirates ID format. Expected: 784-YYYY-NNNNNNN-C"
    )
    String emiratesId,

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    String fullName,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email too long")
    String email,

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^\\+971[0-9]{8,9}$",
        message = "Invalid UAE phone number. Expected format: +971XXXXXXXXX"
    )
    String phone,

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "Gender must be MALE or FEMALE")
    String gender
) {}
