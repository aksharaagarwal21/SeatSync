package com.seatsync.entity;

import com.seatsync.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private final Event event = TestFixtures.event(1, LocalDate.of(2026, 10, 1), EventStatus.ON_SALE);
    private final User user = TestFixtures.user(3, Role.USER);

    @Test
    void totalIsTheSumOfSeatPrices() {
        List<Seat> seats = List.of(
                TestFixtures.seat(1, event, "A", 1, "499.00"),
                TestFixtures.seat(2, event, "A", 2, "499.00"),
                TestFixtures.seat(3, event, "B", 1, "1299.50"));

        assertThat(Booking.calculateTotal(seats)).isEqualByComparingTo("2297.50");
    }

    @Test
    void confirmCapturesSeatPricesAtBookingTime() {
        Seat seat = TestFixtures.seat(1, event, "A", 1, "499.00");
        seat.book();

        Booking booking = Booking.confirm("SS-TEST0001", user, event, List.of(seat), NOW);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo(new BigDecimal("499.00"));
        assertThat(booking.getSeats()).singleElement().satisfies(line -> {
            assertThat(line.isActive()).isTrue();
            assertThat(line.getPrice()).isEqualByComparingTo("499.00");
        });
    }

    @Test
    void confirmRejectsSeatsThatWereNotMarkedBooked() {
        Seat seat = TestFixtures.seat(1, event, "A", 1, "499.00");

        assertThatThrownBy(() -> Booking.confirm("SS-TEST0001", user, event, List.of(seat), NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelReleasesSeatsAndDeactivatesLines() {
        Seat seat = TestFixtures.seat(1, event, "A", 1, "499.00");
        seat.book();
        Booking booking = Booking.confirm("SS-TEST0001", user, event, List.of(seat), NOW);

        booking.cancel(NOW.plusSeconds(60));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(booking.getSeats()).allSatisfy(line -> assertThat(line.isActive()).isFalse());
    }
}
