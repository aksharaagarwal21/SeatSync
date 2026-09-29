package com.seatsync.service;

import com.seatsync.config.BookingProperties;
import com.seatsync.dto.seat.HoldResponse;
import com.seatsync.dto.seat.SeatResponse;
import com.seatsync.dto.seat.SeatSelectionRequest;
import com.seatsync.entity.Booking;
import com.seatsync.entity.Event;
import com.seatsync.entity.Seat;
import com.seatsync.exception.BusinessRuleException;
import com.seatsync.exception.ResourceNotFoundException;
import com.seatsync.exception.SeatAlreadyBookedException;
import com.seatsync.mapper.SeatMapper;
import com.seatsync.repository.EventRepository;
import com.seatsync.repository.SeatRepository;
import com.seatsync.service.locking.SeatLockService;
import com.seatsync.service.realtime.SeatsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Temporary seat holds: while a user reviews their booking, their seats are RESERVED for a few
 * minutes so nobody else can take them. Holds go through the same locking as bookings.
 */
@Service
public class SeatHoldService {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldService.class);

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final SeatLockService seatLockService;
    private final BookingPolicy bookingPolicy;
    private final SeatMapper seatMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingProperties properties;

    public SeatHoldService(EventRepository eventRepository,
                           SeatRepository seatRepository,
                           SeatLockService seatLockService,
                           BookingPolicy bookingPolicy,
                           SeatMapper seatMapper,
                           ApplicationEventPublisher eventPublisher,
                           BookingProperties properties) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.seatLockService = seatLockService;
        this.bookingPolicy = bookingPolicy;
        this.seatMapper = seatMapper;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    /** Replaces the user's current hold for this event with exactly the requested seats. */
    @Transactional
    public HoldResponse holdSeats(Long userId, SeatSelectionRequest request) {
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> ResourceNotFoundException.event(request.eventId()));
        if (!bookingPolicy.isBookable(event)) {
            throw new BusinessRuleException("Booking is closed for this event.");
        }

        Set<Long> requestedIds = new HashSet<>(request.seatIds());
        Set<Long> affectedIds = new HashSet<>(requestedIds);
        affectedIds.addAll(seatRepository.findIdsHeldBy(event.getId(), userId));

        Instant now = bookingPolicy.now();
        List<Seat> seats = seatLockService.acquire(properties.lockingStrategy(), event.getId(), affectedIds);
        List<Seat> requested = seats.stream().filter(seat -> requestedIds.contains(seat.getId())).toList();

        List<String> unavailable = requested.stream()
                .filter(seat -> !seat.isAvailableTo(userId, now))
                .map(Seat::getSeatNumber)
                .toList();
        if (!unavailable.isEmpty()) {
            throw new SeatAlreadyBookedException(unavailable);
        }

        Instant expiresAt = now.plus(properties.holdDuration());
        seats.stream()
                .filter(seat -> !requestedIds.contains(seat.getId()) && seat.isHeldBy(userId, now))
                .forEach(Seat::release);
        requested.forEach(seat -> seat.hold(userId, expiresAt));
        seatLockService.flushSeatChanges();

        eventPublisher.publishEvent(SeatsChangedEvent.of(event.getId(), affectedIds));
        List<SeatResponse> heldSeats = requested.stream().map(seat -> seatMapper.toResponse(seat, userId, now)).toList();
        return new HoldResponse(event.getId(), expiresAt, heldSeats, Booking.calculateTotal(requested));
    }

    @Transactional
    public void releaseHolds(Long userId, Long eventId) {
        List<Long> heldIds = seatRepository.findIdsHeldBy(eventId, userId);
        if (heldIds.isEmpty()) {
            return;
        }
        Instant now = bookingPolicy.now();
        List<Seat> seats = seatLockService.acquire(properties.lockingStrategy(), eventId, heldIds);
        seats.stream().filter(seat -> seat.isHeldBy(userId, now)).forEach(Seat::release);
        seatLockService.flushSeatChanges();
        eventPublisher.publishEvent(SeatsChangedEvent.of(eventId, heldIds));
    }

    /** Lapsed holds are already treated as available; this makes the stored state match. */
    @Transactional
    public int releaseExpiredHolds() {
        Instant now = bookingPolicy.now();
        List<Long> eventIds = seatRepository.findEventIdsWithExpiredHolds(now);
        if (eventIds.isEmpty()) {
            return 0;
        }
        int released = seatRepository.releaseExpiredHolds(now);
        eventIds.forEach(eventId -> eventPublisher.publishEvent(SeatsChangedEvent.of(eventId, List.of())));
        log.debug("Released {} expired seat holds across {} events", released, eventIds.size());
        return released;
    }
}
