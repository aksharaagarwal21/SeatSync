package com.seatsync.service.locking;

import com.seatsync.entity.EventStatus;
import com.seatsync.entity.Seat;
import com.seatsync.exception.BookingConflictException;
import com.seatsync.exception.ResourceNotFoundException;
import com.seatsync.repository.SeatRepository;
import com.seatsync.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;

@ExtendWith(MockitoExtension.class)
class SeatLockServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatLockService seatLockService;

    private final List<Seat> seats = List.of(
            TestFixtures.seat(3, TestFixtures.event(1, LocalDate.of(2026, 10, 1), EventStatus.ON_SALE), "A", 3, "499"),
            TestFixtures.seat(7, TestFixtures.event(1, LocalDate.of(2026, 10, 1), EventStatus.ON_SALE), "A", 7, "499"));

    @Test
    void pessimisticStrategyLocksSeatsInAscendingIdOrder() {
        when(seatRepository.lockForBooking(1L, List.of(3L, 7L))).thenReturn(seats);

        assertThat(seatLockService.acquire(LockingStrategy.PESSIMISTIC, 1L, List.of(7L, 3L, 7L))).isEqualTo(seats);
        verify(seatRepository, never()).findForBooking(any(), anyCollection());
    }

    @Test
    void optimisticStrategyReadsWithoutLocks() {
        when(seatRepository.findForBooking(1L, List.of(3L, 7L))).thenReturn(seats);

        assertThat(seatLockService.acquire(LockingStrategy.OPTIMISTIC, 1L, List.of(3L, 7L))).isEqualTo(seats);
        verify(seatRepository, never()).lockForBooking(any(), anyCollection());
    }

    @Test
    void missingSeatsAreReportedAsNotFound() {
        when(seatRepository.findForBooking(1L, List.of(3L, 7L, 9L))).thenReturn(seats);

        assertThatThrownBy(() -> seatLockService.acquire(LockingStrategy.OPTIMISTIC, 1L, List.of(3L, 7L, 9L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void staleVersionOnFlushBecomesBookingConflict() {
        doThrow(new ObjectOptimisticLockingFailureException(Seat.class, 3L)).when(seatRepository).flush();

        assertThatThrownBy(seatLockService::flushSeatChanges)
                .isInstanceOf(BookingConflictException.class)
                .hasMessage(BookingConflictException.SEATS_UNAVAILABLE);
    }
}
