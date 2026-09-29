package com.seatsync.repository.projection;

import java.math.BigDecimal;

public record EventSeatStats(
        Long eventId,
        Long totalSeats,
        Long availableSeats,
        Long heldSeats,
        Long bookedSeats,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {

    public static EventSeatStats empty(Long eventId) {
        return new EventSeatStats(eventId, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
