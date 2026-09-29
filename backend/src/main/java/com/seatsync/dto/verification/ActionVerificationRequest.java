package com.seatsync.dto.verification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seatsync.entity.OtpPurpose;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Asks for a code to approve a booking (eventId + seatIds) or a cancellation (bookingId). */
public record ActionVerificationRequest(
        @NotNull(message = "Purpose is required")
        OtpPurpose purpose,

        @Positive
        Long eventId,

        @Size(max = 10, message = "You can book up to 10 seats at a time")
        List<@NotNull @Positive Long> seatIds,

        @Positive
        Long bookingId
) {

    @JsonIgnore
    @AssertTrue(message = "Provide eventId and seatIds for a booking, or bookingId for a cancellation")
    public boolean isCompleteForPurpose() {
        if (purpose == null) {
            return true;
        }
        return switch (purpose) {
            case BOOKING -> eventId != null && seatIds != null && !seatIds.isEmpty();
            case CANCELLATION -> bookingId != null;
            case LOGIN, REGISTRATION -> false;
        };
    }
}
