package com.seatsync.dto.verification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.seatsync.entity.OtpPurpose;

import java.time.Instant;
import java.util.UUID;

/**
 * Tells the client a code was emailed. When two-step verification is switched off,
 * {@code required} is false and the client proceeds without a code.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerificationChallengeResponse(
        boolean required,
        UUID challengeId,
        OtpPurpose purpose,
        String maskedEmail,
        Instant expiresAt,
        Instant resendAvailableAt
) {

    public static VerificationChallengeResponse notRequired() {
        return new VerificationChallengeResponse(false, null, null, null, null, null);
    }
}
