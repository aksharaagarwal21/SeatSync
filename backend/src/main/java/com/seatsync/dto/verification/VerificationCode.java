package com.seatsync.dto.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/** The code a user typed, and the challenge it answers. */
public record VerificationCode(
        @NotNull(message = "Verification challenge is required")
        UUID challengeId,

        @NotBlank(message = "Enter the 6-digit code")
        @Pattern(regexp = "\\d{6}", message = "The code is 6 digits")
        String code
) {
}
