package com.seatsync.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @param enabled             require emailed codes for sign-in, booking and cancellation. Disable
 *                            only for automated load tests.
 * @param codeTtl             how long a code stays valid
 * @param maxAttempts         wrong guesses allowed per code before it locks
 * @param resendCooldown      minimum wait between sends of the same challenge
 * @param maxSends            sends allowed per challenge (first send + resends)
 * @param maxChallenges       new challenges a user may start within {@code challengeWindow}
 * @param challengeWindow     rate-limit window
 * @param from                sender address for verification emails
 */
@Validated
@ConfigurationProperties(prefix = "seatsync.otp")
public record OtpProperties(
        boolean enabled,
        @NotNull Duration codeTtl,
        @Min(1) int maxAttempts,
        @NotNull Duration resendCooldown,
        @Min(1) int maxSends,
        @Min(1) int maxChallenges,
        @NotNull Duration challengeWindow,
        @NotBlank String from
) {
}
