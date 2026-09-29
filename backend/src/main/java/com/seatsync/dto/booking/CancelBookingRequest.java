package com.seatsync.dto.booking;

import com.seatsync.dto.verification.VerificationCode;
import jakarta.validation.Valid;

/** {@code verification} is the emailed code approving this cancellation; required when two-step verification is on. */
public record CancelBookingRequest(@Valid VerificationCode verification) {
}
