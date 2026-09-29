package com.seatsync.dto.seat;

import com.seatsync.entity.SeatSection;
import com.seatsync.entity.SeatStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record SeatResponse(
        Long id,
        String seatNumber,
        String row,
        int number,
        SeatSection section,
        BigDecimal price,
        SeatStatus status,
        boolean heldByMe,
        Instant holdExpiresAt
) {
}
