package com.seatsync.dto.admin;

import com.seatsync.dto.booking.BookingDisplayStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdminBookingResponse(
        Long id,
        String reference,
        String customerName,
        String customerEmail,
        Long eventId,
        String eventName,
        LocalDate eventDate,
        List<String> seatNumbers,
        BigDecimal totalAmount,
        BookingDisplayStatus status,
        Instant bookingTime
) {
}
