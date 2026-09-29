package com.seatsync.dto.seat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record HoldResponse(Long eventId, Instant expiresAt, List<SeatResponse> seats, BigDecimal totalAmount) {
}
