package com.seatsync.service.verification;

import com.seatsync.config.OtpProperties;
import com.seatsync.dto.verification.VerificationCode;
import com.seatsync.entity.OtpChallenge;
import com.seatsync.entity.OtpPurpose;
import com.seatsync.exception.VerificationFailedException;
import com.seatsync.repository.OtpChallengeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/**
 * Checks a code in its own transaction. A wrong guess must be counted even though the request
 * fails, so this commits independently of the caller (REQUIRES_NEW, no rollback on failure).
 */
@Component
public class OtpVerifier {

    static final String INVALID_MESSAGE = "This verification code is invalid or has expired. Request a new code.";

    private final OtpChallengeRepository challengeRepository;
    private final OtpCodes codes;
    private final OtpProperties properties;
    private final Clock clock;

    public OtpVerifier(OtpChallengeRepository challengeRepository, OtpCodes codes, OtpProperties properties, Clock clock) {
        this.challengeRepository = challengeRepository;
        this.codes = codes;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * @param expectedUserId the signed-in user who must own the challenge, or null during sign-in
     * @return the id of the user the code belongs to
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = VerificationFailedException.class)
    public Long verify(VerificationCode submitted, Long expectedUserId, Set<OtpPurpose> purposes, String context) {
        OtpChallenge challenge = challengeRepository.lockById(submitted.challengeId())
                .orElseThrow(() -> new VerificationFailedException(INVALID_MESSAGE));
        Long ownerId = challenge.getUser().getId();
        if (expectedUserId != null && !expectedUserId.equals(ownerId)) {
            throw new VerificationFailedException(INVALID_MESSAGE);
        }
        if (purposes.stream().noneMatch(purpose -> challenge.matches(purpose, context))) {
            throw new VerificationFailedException("This code was issued for a different action. Request a new code.");
        }

        Instant now = clock.instant();
        if (challenge.isConsumed()) {
            throw new VerificationFailedException("This code has already been used. Request a new code.");
        }
        if (challenge.isExpired(now)) {
            throw new VerificationFailedException("This code has expired. Request a new code.");
        }
        if (challenge.getAttempts() >= properties.maxAttempts()) {
            throw new VerificationFailedException("Too many incorrect attempts. Request a new code.");
        }
        if (!codes.matches(submitted.code(), challenge.getCodeHash())) {
            challenge.recordFailedAttempt();
            int remaining = properties.maxAttempts() - challenge.getAttempts();
            throw new VerificationFailedException(remaining > 0
                    ? "That code is incorrect. " + remaining + (remaining == 1 ? " attempt" : " attempts") + " left."
                    : "Too many incorrect attempts. Request a new code.");
        }

        challenge.markVerified(now);
        return ownerId;
    }
}
