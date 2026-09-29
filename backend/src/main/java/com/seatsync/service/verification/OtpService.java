package com.seatsync.service.verification;

import com.seatsync.config.OtpProperties;
import com.seatsync.dto.verification.VerificationChallengeResponse;
import com.seatsync.entity.OtpChallenge;
import com.seatsync.entity.OtpPurpose;
import com.seatsync.entity.User;
import com.seatsync.exception.TooManyRequestsException;
import com.seatsync.exception.VerificationFailedException;
import com.seatsync.repository.OtpChallengeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Issues, resends and consumes one-time codes. Checking a code lives in {@link OtpVerifier}. */
@Service
public class OtpService {

    private final OtpChallengeRepository challengeRepository;
    private final OtpCodes codes;
    private final OtpNotifier notifier;
    private final OtpProperties properties;
    private final Clock clock;

    public OtpService(OtpChallengeRepository challengeRepository,
                      OtpCodes codes,
                      OtpNotifier notifier,
                      OtpProperties properties,
                      Clock clock) {
        this.challengeRepository = challengeRepository;
        this.codes = codes;
        this.notifier = notifier;
        this.properties = properties;
        this.clock = clock;
    }

    /** Creates a challenge bound to {@code context} and emails its code. */
    @Transactional
    public VerificationChallengeResponse issue(User user, OtpPurpose purpose, String context, String summary) {
        Instant now = clock.instant();
        long recent = challengeRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minus(properties.challengeWindow()));
        if (recent >= properties.maxChallenges()) {
            throw new TooManyRequestsException("Too many verification codes requested. Please wait a few minutes and try again.");
        }
        String code = codes.generate();
        OtpChallenge challenge = challengeRepository.save(
                new OtpChallenge(user, purpose, context, summary, codes.hash(code), now, now.plus(properties.codeTtl())));
        notifier.send(user, purpose, code, summary, properties.codeTtl());
        return toResponse(challenge);
    }

    /** Sends a fresh code for the same challenge, after a cooldown and up to a limit. */
    @Transactional
    public VerificationChallengeResponse resend(UUID challengeId) {
        OtpChallenge challenge = challengeRepository.lockById(challengeId)
                .orElseThrow(() -> new VerificationFailedException(OtpVerifier.INVALID_MESSAGE));
        Instant now = clock.instant();
        if (challenge.isConsumed()) {
            throw new VerificationFailedException("This code has already been used.");
        }
        if (challenge.getSendCount() >= properties.maxSends()) {
            throw new TooManyRequestsException("You've reached the limit for this code. Go back and start again to get a new one.");
        }
        Instant availableAt = resendAvailableAt(challenge);
        if (now.isBefore(availableAt)) {
            long seconds = Math.max(1, Duration.between(now, availableAt).toSeconds());
            throw new TooManyRequestsException("You can request a new code in " + seconds + " seconds.");
        }
        String code = codes.generate();
        challenge.resend(codes.hash(code), now, now.plus(properties.codeTtl()));
        notifier.send(challenge.getUser(), challenge.getPurpose(), code, challenge.getSummary(), properties.codeTtl());
        return toResponse(challenge);
    }

    /**
     * Marks a verified challenge as used, in the caller's transaction. If the action it approved
     * rolls back, so does this, and the user can retry with the same code.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void consume(UUID challengeId) {
        if (challengeRepository.consume(challengeId, clock.instant()) == 0) {
            throw new VerificationFailedException("This code has already been used. Request a new code.");
        }
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    @Transactional
    public void deleteExpiredChallenges() {
        challengeRepository.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
    }

    private VerificationChallengeResponse toResponse(OtpChallenge challenge) {
        return new VerificationChallengeResponse(
                true,
                challenge.getId(),
                challenge.getPurpose(),
                OtpCodes.maskEmail(challenge.getUser().getEmail()),
                challenge.getExpiresAt(),
                resendAvailableAt(challenge));
    }

    private Instant resendAvailableAt(OtpChallenge challenge) {
        return challenge.getLastSentAt().plus(properties.resendCooldown());
    }
}
