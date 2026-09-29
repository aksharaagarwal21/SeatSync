package com.seatsync.dto.admin;

import com.seatsync.entity.EventCategory;
import com.seatsync.entity.EventStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record AdminEventResponse(
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
        int rows,
        int seatsPerRow,
        SectionPricing pricing,
        long totalSeats,
        long availableSeats,
        long heldSeats,
        long bookedSeats
) {
}
