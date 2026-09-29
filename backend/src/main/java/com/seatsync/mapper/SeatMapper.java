package com.seatsync.mapper;

import com.seatsync.dto.seat.SeatResponse;
import com.seatsync.entity.Seat;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SeatMapper {

    /** Hides other users' identities: callers only learn whether a hold is their own. */
    public SeatResponse toResponse(Seat seat, Long viewerId, Instant now) {
        boolean heldByMe = viewerId != null && seat.isHeldBy(viewerId, now);
        return new SeatResponse(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getRow(),
                seat.getNumber(),
                seat.getSection(),
                seat.getPrice(),
                seat.effectiveStatus(now),
                heldByMe,
                heldByMe ? seat.getHoldExpiresAt() : null);
    }
}
