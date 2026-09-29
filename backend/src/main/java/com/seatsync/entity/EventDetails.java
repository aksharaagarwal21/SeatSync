package com.seatsync.entity;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventDetails(
        String name,
        String description,
        EventCategory category,
        String venue,
        String city,
        LocalDate eventDate,
        LocalTime startTime,
        String imageUrl
) {
}
