package com.seatsync.dto.event;

import com.seatsync.entity.EventCategory;
import com.seatsync.entity.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventSummaryResponse(
        Long id,
        String name,
        EventCategory category,
        String venue,
        String city,
        LocalDate eventDate,
        LocalTime startTime,
        String imageUrl,
        EventStatus status,
        boolean bookingOpen,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        long totalSeats,
        long availableSeats
) {
}
