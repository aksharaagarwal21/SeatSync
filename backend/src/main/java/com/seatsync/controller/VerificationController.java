package com.seatsync.controller;

import com.seatsync.dto.verification.ActionVerificationRequest;
import com.seatsync.dto.verification.VerificationChallengeResponse;
import com.seatsync.security.AuthenticatedUser;
import com.seatsync.service.verification.OtpService;
import com.seatsync.service.verification.VerificationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/verifications")
public class VerificationController {

    private final VerificationService verificationService;
    private final OtpService otpService;

    public VerificationController(VerificationService verificationService, OtpService otpService) {
        this.verificationService = verificationService;
        this.otpService = otpService;
    }

    /** Emails a code approving a booking or cancellation. */
    @PostMapping
    public VerificationChallengeResponse requestCode(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @Valid @RequestBody ActionVerificationRequest request) {
        return verificationService.requestActionCode(user.id(), user.isAdmin(), request);
    }

    /** Public so sign-in can resend; the code still only ever goes to the account's own email. */
    @PostMapping("/{challengeId}/resend")
    public VerificationChallengeResponse resend(@PathVariable UUID challengeId) {
        return otpService.resend(challengeId);
    }
}
