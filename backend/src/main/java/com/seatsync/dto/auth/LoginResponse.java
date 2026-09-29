package com.seatsync.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.seatsync.dto.verification.VerificationChallengeResponse;

import java.time.Instant;

/**
 * Either a signed-in session ({@code token}, {@code user}) or, with two-step verification on,
 * the challenge whose emailed code must be confirmed at {@code POST /api/auth/verify}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(String token, Instant expiresAt, UserResponse user, VerificationChallengeResponse verification) {

    public static LoginResponse authenticated(String token, Instant expiresAt, UserResponse user) {
        return new LoginResponse(token, expiresAt, user, null);
    }

    public static LoginResponse verificationRequired(VerificationChallengeResponse verification) {
        return new LoginResponse(null, null, null, verification);
    }

    public boolean isAuthenticated() {
        return token != null;
    }
}
