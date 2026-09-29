package com.seatsync.service;

import com.seatsync.config.BookingProperties;
import com.seatsync.dto.booking.BookingDisplayStatus;
import com.seatsync.entity.Booking;
import com.seatsync.entity.BookingStatus;
import com.seatsync.entity.Event;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/** Time-based booking rules in one place: when an event is bookable and when a booking can be cancelled. */
@Component
public class BookingPolicy {

    private final Clock clock;
    private final BookingProperties properties;

    public BookingPolicy(Clock clock, BookingProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public Instant now() {
        return clock.instant();
    }

    public boolean isBookable(Event event) {
        return event.isBookable(now(), clock.getZone());
    }

    public boolean hasStarted(Event event) {
        return !event.startsAt(clock.getZone()).isAfter(now());
    }

    public boolean isCancellable(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
                && booking.getEvent().startsAt(clock.getZone()).isAfter(now().plus(properties.cancellationCutoff()));
    }

    public long cancellationCutoffHours() {
        return properties.cancellationCutoff().toHours();
    }

    public BookingDisplayStatus displayStatus(Booking booking) {
        return switch (booking.getStatus()) {
            case PENDING -> BookingDisplayStatus.PENDING;
            case CANCELLED -> BookingDisplayStatus.CANCELLED;
            case CONFIRMED -> hasStarted(booking.getEvent()) ? BookingDisplayStatus.COMPLETED : BookingDisplayStatus.CONFIRMED;
        };
    }
}
