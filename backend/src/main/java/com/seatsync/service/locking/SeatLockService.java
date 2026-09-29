package com.seatsync.service.locking;

import com.seatsync.entity.Seat;
import com.seatsync.exception.BookingConflictException;
import com.seatsync.exception.ResourceNotFoundException;
import com.seatsync.repository.SeatRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Acquires the seats a hold or booking will modify, using the requested locking strategy.
 * Must run inside the caller's transaction so pessimistic row locks live until its commit.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class SeatLockService {

    private final SeatRepository seatRepository;

    public SeatLockService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    /**
     * Returns every requested seat of the event, ordered by id. Sorting the ids up front gives
     * every transaction the same lock acquisition order, which prevents lock-order deadlocks.
     */
    public List<Seat> acquire(LockingStrategy strategy, Long eventId, Collection<Long> seatIds) {
        List<Long> orderedIds = seatIds.stream().distinct().sorted().toList();
        List<Seat> seats = switch (strategy) {
            case OPTIMISTIC -> seatRepository.findForBooking(eventId, orderedIds);
            case PESSIMISTIC -> seatRepository.lockForBooking(eventId, orderedIds);
        };
        if (seats.size() != orderedIds.size()) {
            throw new ResourceNotFoundException("One or more selected seats do not exist for this event.");
        }
        return seats;
    }

    /**
     * Writes pending seat changes now so a stale {@code @Version} is detected inside the service
     * (and mapped to a clean 409) rather than at commit time.
     */
    public void flushSeatChanges() {
        try {
            seatRepository.flush();
        } catch (OptimisticLockingFailureException ex) {
            throw new BookingConflictException(BookingConflictException.SEATS_UNAVAILABLE, ex);
        }
    }
}
