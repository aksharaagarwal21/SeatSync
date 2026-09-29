package com.seatsync.concurrency;

import com.seatsync.dto.booking.CreateBookingRequest;
import com.seatsync.exception.BookingConflictException;
import com.seatsync.exception.SeatAlreadyBookedException;
import com.seatsync.service.BookingService;
import com.seatsync.service.locking.LockingStrategy;
import com.seatsync.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real threads, real transactions, real PostgreSQL row locks. Every scenario runs against both
 * locking strategies and finishes by checking the database directly for double bookings.
 */
class SeatBookingConcurrencyTest extends AbstractIntegrationTest {

    private static final int THREADS = 100;

    @Autowired
    private BookingService bookingService;

    @ParameterizedTest(name = "{0}")
    @EnumSource(LockingStrategy.class)
    @DisplayName("100 users racing for the same seat: exactly one wins")
    void hundredUsersRaceForTheSameSeat(LockingStrategy strategy) throws Exception {
        Long eventId = createEvent(1, 10).id();
        Long contestedSeat = seatIds(eventId).getFirst();
        List<Long> users = createUsers(THREADS);

        Outcome outcome = race(users.size(), i ->
                bookingService.createBooking(users.get(i), new CreateBookingRequest(eventId, List.of(contestedSeat)), strategy));

        assertThat(outcome.successes()).isEqualTo(1);
        assertThat(outcome.conflicts()).isEqualTo(THREADS - 1);
        assertThat(outcome.unexpected()).isEmpty();
        assertThat(confirmedBookingsForSeat(contestedSeat)).isEqualTo(1);
        assertThat(seatStatus(contestedSeat)).isEqualTo("BOOKED");
        assertNoDoubleBookings();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LockingStrategy.class)
    @DisplayName("users booking different seats all succeed")
    void usersBookingDifferentSeatsAllSucceed(LockingStrategy strategy) throws Exception {
        Long eventId = createEvent(5, 10).id();
        List<Long> seats = seatIds(eventId);
        List<Long> users = createUsers(seats.size());

        Outcome outcome = race(seats.size(), i ->
                bookingService.createBooking(users.get(i), new CreateBookingRequest(eventId, List.of(seats.get(i))), strategy));

        assertThat(outcome.successes()).isEqualTo(seats.size());
        assertThat(outcome.conflicts()).isZero();
        assertThat(outcome.unexpected()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seats WHERE event_id = ? AND status = 'BOOKED'", Long.class, eventId))
                .isEqualTo(seats.size());
        assertNoDoubleBookings();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LockingStrategy.class)
    @DisplayName("overlapping seat sets never deadlock and never partially book")
    void overlappingSeatSetsAreAllOrNothing(LockingStrategy strategy) throws Exception {
        Long eventId = createEvent(1, 12).id();
        List<Long> seats = seatIds(eventId);
        List<Long> users = createUsers(60);

        // Each user wants a sliding window of 3 seats, requested in shuffled order to provoke lock-order deadlocks.
        List<List<Long>> requests = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            int start = i % (seats.size() - 2);
            List<Long> window = new ArrayList<>(seats.subList(start, start + 3));
            Collections.shuffle(window);
            requests.add(window);
        }

        Outcome outcome = race(users.size(), i ->
                bookingService.createBooking(users.get(i), new CreateBookingRequest(eventId, requests.get(i)), strategy));

        assertThat(outcome.unexpected()).as("no deadlocks or other failures").isEmpty();
        assertThat(outcome.successes()).isPositive();
        assertThat(outcome.successes() + outcome.conflicts()).isEqualTo(users.size());
        assertNoDoubleBookings();

        Long bookedSeats = jdbc.queryForObject("SELECT count(*) FROM seats WHERE event_id = ? AND status = 'BOOKED'", Long.class, eventId);
        Long activeLines = jdbc.queryForObject("""
                SELECT count(*) FROM booking_seats bs JOIN bookings b ON b.id = bs.booking_id
                WHERE b.event_id = ? AND b.status = 'CONFIRMED' AND bs.active
                """, Long.class, eventId);
        assertThat(activeLines).as("every booked seat belongs to exactly one confirmed booking").isEqualTo(bookedSeats);
        assertThat(activeLines).isEqualTo(outcome.successes() * 3L);
    }

    private Outcome race(int tasks, IndexedTask task) throws Exception {
        CountDownLatch ready = new CountDownLatch(tasks);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(tasks)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                int index = i;
                futures.add(executor.submit((Callable<Void>) () -> {
                    ready.countDown();
                    start.await();
                    try {
                        task.run(index);
                        successes.incrementAndGet();
                    } catch (SeatAlreadyBookedException | BookingConflictException ex) {
                        conflicts.incrementAndGet();
                    } catch (Throwable ex) {
                        unexpected.add(ex);
                    }
                    return null;
                }));
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        }
        return new Outcome(successes.get(), conflicts.get(), List.copyOf(unexpected));
    }

    private long confirmedBookingsForSeat(Long seatId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM booking_seats bs JOIN bookings b ON b.id = bs.booking_id
                WHERE bs.seat_id = ? AND b.status = 'CONFIRMED'
                """, Long.class, seatId);
    }

    private String seatStatus(Long seatId) {
        return jdbc.queryForObject("SELECT status FROM seats WHERE id = ?", String.class, seatId);
    }

    /** The same query the load-test verification script runs: must return zero rows. */
    private void assertNoDoubleBookings() {
        List<Long> doubleBooked = jdbc.queryForList("""
                SELECT bs.seat_id FROM booking_seats bs JOIN bookings b ON b.id = bs.booking_id
                WHERE b.status = 'CONFIRMED'
                GROUP BY bs.seat_id HAVING count(*) > 1
                """, Long.class);
        assertThat(doubleBooked).isEmpty();
    }

    @FunctionalInterface
    private interface IndexedTask {
        void run(int index);
    }

    private record Outcome(int successes, int conflicts, List<Throwable> unexpected) {
    }
}
