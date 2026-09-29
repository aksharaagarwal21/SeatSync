package com.seatsync.dto.seat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Seats a user wants to hold. Bookings use {@link com.seatsync.dto.booking.CreateBookingRequest}. */
public record SeatSelectionRequest(
        @NotNull(message = "Event is required")
        @Positive
        Long eventId,

        @NotEmpty(message = "Select at least one seat")
        @Size(max = 10, message = "You can select up to 10 seats at a time")
        List<@NotNull @Positive Long> seatIds
) {
}
