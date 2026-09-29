package com.seatsync.dto.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record BookingResponse(
        Long id,
        String reference,
        Long eventId,
        String eventName,
        String venue,
        String city,
        LocalDate eventDate,
        LocalTime startTime,
        String imageUrl,
        List<BookedSeatResponse> seats,
        BigDecimal totalAmount,
        BookingDisplayStatus status,
        Instant bookingTime,
        boolean cancellable
) {
}
