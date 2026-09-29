package com.seatsync.concurrency;

import com.seatsync.dto.booking.CreateBookingRequest;
import com.seatsync.entity.Seat;
import com.seatsync.exception.SeatAlreadyBookedException;
import com.seatsync.repository.SeatRepository;
import com.seatsync.service.BookingService;
import com.seatsync.service.locking.LockingStrategy;
import com.seatsync.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Demonstrates each locking mechanism in isolation, deterministically. */
class LockingMechanismTest extends AbstractIntegrationTest {

    @Autowired private SeatRepository seatRepository;
    @Autowired private BookingService bookingService;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void optimisticLockRejectsUpdateBasedOnStaleVersion() {
        Long eventId = createEvent(1, 5).id();
        Long seatId = seatIds(eventId).getFirst();
        TransactionTemplate userB = new TransactionTemplate(transactionManager);
        TransactionTemplate userA = new TransactionTemplate(transactionManager);
        userA.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> userB.executeWithoutResult(status -> {
            Seat staleCopy = seatRepository.findById(seatId).orElseThrow();
            assertThat(staleCopy.getVersion()).isZero();

            // User A reads the same version, books the seat and commits: version 0 -> 1.
            userA.executeWithoutResult(inner -> seatRepository.findById(seatId).orElseThrow().book());

            // User B still holds version 0; Hibernate issues UPDATE ... WHERE version = 0 and matches no row.
            staleCopy.book();
            seatRepository.flush();
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(jdbc.queryForObject("SELECT version FROM seats WHERE id = ?", Long.class, seatId)).isEqualTo(1L);
    }

    @Test
    void pessimisticLockMakesCompetingTransactionWaitThenSeeBookedSeat() throws Exception {
        Long eventId = createEvent(1, 5).id();
        Long seatId = seatIds(eventId).getFirst();
        List<Long> users = createUsers(2);
        CountDownLatch lockHeld = new CountDownLatch(1);
        Duration holdLockFor = Duration.ofMillis(800);

        CompletableFuture<Void> lockHolder = CompletableFuture.runAsync(() ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    Seat seat = seatRepository.lockForBooking(eventId, List.of(seatId)).getFirst();
                    lockHeld.countDown();
                    sleep(holdLockFor);
                    seat.book();
                }));

        assertThat(lockHeld.await(10, TimeUnit.SECONDS)).isTrue();
        long started = System.nanoTime();
        assertThatThrownBy(() -> bookingService.createBooking(
                users.get(1), new CreateBookingRequest(eventId, List.of(seatId)), LockingStrategy.PESSIMISTIC))
                .isInstanceOf(SeatAlreadyBookedException.class);
        Duration waited = Duration.ofNanos(System.nanoTime() - started);

        lockHolder.get(10, TimeUnit.SECONDS);
        assertThat(waited).as("second transaction blocked on the row lock").isGreaterThanOrEqualTo(holdLockFor.minusMillis(200));
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
