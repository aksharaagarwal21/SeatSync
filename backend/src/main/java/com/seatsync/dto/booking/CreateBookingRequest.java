package com.seatsync.dto.booking;

import com.seatsync.dto.verification.VerificationCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** {@code verification} is the emailed code approving exactly these seats; required when two-step verification is on. */
public record CreateBookingRequest(
        @NotNull(message = "Event is required")
        @Positive
        Long eventId,

        @NotEmpty(message = "Select at least one seat")
        @Size(max = 10, message = "You can book up to 10 seats at a time")
        List<@NotNull @Positive Long> seatIds,

        @Valid
        VerificationCode verification
) {

    public CreateBookingRequest(Long eventId, List<Long> seatIds) {
        this(eventId, seatIds, null);
    }
}
