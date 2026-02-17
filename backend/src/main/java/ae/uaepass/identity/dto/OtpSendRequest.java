package ae.uaepass.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record OtpSendRequest(
    @NotNull(message = "User ID is required")
    UUID userId,

    @NotBlank(message = "Channel is required")
    @Pattern(regexp = "^(SMS|EMAIL)$", message = "Channel must be SMS or EMAIL")
    String channel
) {}
