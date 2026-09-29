package com.seatsync.dto.event;

import com.seatsync.entity.EventCategory;
import com.seatsync.entity.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventDetailResponse(
        Long id,
        String name,
        String description,
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
        long availableSeats,
        List<SectionAvailabilityResponse> sections
) {
}
