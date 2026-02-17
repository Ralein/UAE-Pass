package ae.uaepass.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record PinCreateRequest(
    @NotNull(message = "User ID is required")
    UUID userId,

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must be exactly 6 digits")
    String pin,

    @NotBlank(message = "PIN confirmation is required")
    @Pattern(regexp = "^\\d{6}$", message = "PIN confirmation must be exactly 6 digits")
    String pinConfirm
) {}
