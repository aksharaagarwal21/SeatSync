package com.seatsync.service.verification;

import com.seatsync.config.OtpProperties;
import com.seatsync.dto.verification.VerificationCode;
import com.seatsync.entity.OtpPurpose;
import com.seatsync.exception.VerificationFailedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

/**
 * Gate for sensitive actions. Call it inside the action's transaction: the code is checked
 * (committing any failed attempt on its own), then consumed in the caller's transaction, so a
 * booking that rolls back leaves the code usable for a retry.
 */
@Component
public class ActionVerification {

    private final OtpVerifier verifier;
    private final OtpService otpService;
    private final OtpProperties properties;

    public ActionVerification(OtpVerifier verifier, OtpService otpService, OtpProperties properties) {
        this.verifier = verifier;
        this.otpService = otpService;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void require(Long userId, OtpPurpose purpose, String context, VerificationCode code) {
        if (!properties.enabled()) {
            return;
        }
        if (code == null) {
            throw new VerificationFailedException("Enter the 6-digit code we emailed you to continue.");
        }
        verifier.verify(code, userId, Set.of(purpose), context);
        otpService.consume(code.challengeId());
    }

    public static String bookingContext(Long eventId, Collection<Long> seatIds) {
        return "event:" + eventId + "|seats:" + String.join(",",
                seatIds.stream().distinct().sorted().map(String::valueOf).toList());
    }

    public static String cancellationContext(Long bookingId) {
        return "booking:" + bookingId;
    }
}
