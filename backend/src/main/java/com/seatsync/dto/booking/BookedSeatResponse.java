package com.seatsync.dto.booking;

import com.seatsync.entity.SeatSection;

import java.math.BigDecimal;

public record BookedSeatResponse(Long seatId, String seatNumber, SeatSection section, BigDecimal price) {
}
