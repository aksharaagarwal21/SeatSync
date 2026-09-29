package com.seatsync.dto.event;

import com.seatsync.entity.SeatSection;

import java.math.BigDecimal;

public record SectionAvailabilityResponse(SeatSection section, BigDecimal price, long totalSeats, long availableSeats) {
}
