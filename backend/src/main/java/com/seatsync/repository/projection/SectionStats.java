package com.seatsync.repository.projection;

import com.seatsync.entity.SeatSection;

import java.math.BigDecimal;

public record SectionStats(SeatSection section, BigDecimal price, Long totalSeats, Long availableSeats) {
}
