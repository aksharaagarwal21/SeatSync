package com.seatsync.service.realtime;

import java.util.Collection;
import java.util.List;

/** Published inside a transaction; broadcast to browsers only after that transaction commits. */
public record SeatsChangedEvent(Long eventId, List<Long> seatIds) {

    public static SeatsChangedEvent of(Long eventId, Collection<Long> seatIds) {
        return new SeatsChangedEvent(eventId, List.copyOf(seatIds));
    }
}
