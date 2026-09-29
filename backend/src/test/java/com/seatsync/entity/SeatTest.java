package com.seatsync.entity;

import com.seatsync.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SeatTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private final Event event = TestFixtures.event(1, LocalDate.of(2026, 10, 1), EventStatus.ON_SALE);

    @Test
    void availableSeatCanBeTakenByAnyone() {
        Seat seat = TestFixtures.seat(1, event, "A", 1, "499");

        assertThat(seat.isAvailableTo(7L, NOW)).isTrue();
        assertThat(seat.effectiveStatus(NOW)).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void activeHoldBlocksOtherUsersButNotTheHolder() {
        Seat seat = TestFixtures.seat(1, event, "A", 1, "499");
        seat.hold(7L, NOW.plusSeconds(300));

        assertThat(seat.isAvailableTo(7L, NOW)).isTrue();
        assertThat(seat.isAvailableTo(8L, NOW)).isFalse();
        assertThat(seat.isHeldBy(7L, NOW)).isTrue();
        assertThat(seat.effectiveStatus(NOW)).isEqualTo(SeatStatus.RESERVED);
    }

    @Test
    void expiredHoldIsTreatedAsAvailable() {
        Seat seat = TestFixtures.seat(1, event, "A", 1, "499");
        seat.hold(7L, NOW.minusSeconds(1));

        assertThat(seat.isAvailableTo(8L, NOW)).isTrue();
        assertThat(seat.isHeldBy(7L, NOW)).isFalse();
        assertThat(seat.effectiveStatus(NOW)).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void bookedSeatIsNeverAvailableAndClearsHold() {
        Seat seat = TestFixtures.seat(1, event, "A", 1, "499");
        seat.hold(7L, NOW.plusSeconds(300));
        seat.book();

        assertThat(seat.isAvailableTo(7L, NOW)).isFalse();
        assertThat(seat.getHeldByUserId()).isNull();
        assertThat(seat.getHoldExpiresAt()).isNull();
    }
}
