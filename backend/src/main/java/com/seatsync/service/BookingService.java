package com.seatsync.service;

import com.seatsync.config.BookingProperties;
import com.seatsync.dto.PageResponse;
import com.seatsync.dto.admin.AdminBookingResponse;
import com.seatsync.dto.booking.BookingResponse;
import com.seatsync.dto.booking.CreateBookingRequest;
import com.seatsync.entity.Booking;
import com.seatsync.entity.BookingStatus;
import com.seatsync.entity.Event;
import com.seatsync.entity.Seat;
import com.seatsync.exception.BookingConflictException;
import com.seatsync.exception.BusinessRuleException;
import com.seatsync.exception.ResourceNotFoundException;
import com.seatsync.exception.SeatAlreadyBookedException;
import com.seatsync.mapper.BookingMapper;
import com.seatsync.repository.BookingRepository;
import com.seatsync.repository.BookingSpecifications;
import com.seatsync.repository.EventRepository;
import com.seatsync.repository.UserRepository;
import com.seatsync.security.AuthenticatedUser;
import com.seatsync.service.locking.LockingStrategy;
import com.seatsync.service.locking.SeatLockService;
import com.seatsync.service.realtime.SeatsChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BookingService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final SeatLockService seatLockService;
    private final BookingPolicy bookingPolicy;
    private final BookingReferenceGenerator referenceGenerator;
    private final BookingMapper bookingMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingProperties properties;

    public BookingService(EventRepository eventRepository,
                          UserRepository userRepository,
                          BookingRepository bookingRepository,
                          SeatLockService seatLockService,
                          BookingPolicy bookingPolicy,
                          BookingReferenceGenerator referenceGenerator,
                          BookingMapper bookingMapper,
                          ApplicationEventPublisher eventPublisher,
                          BookingProperties properties) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.seatLockService = seatLockService;
        this.bookingPolicy = bookingPolicy;
        this.referenceGenerator = referenceGenerator;
        this.bookingMapper = bookingMapper;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request) {
        return createBooking(userId, request, properties.lockingStrategy());
    }

    /**
     * Books all requested seats or none of them. Any unavailable seat, version conflict or
     * constraint violation throws, and the whole transaction rolls back.
     */
    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request, LockingStrategy strategy) {
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> ResourceNotFoundException.event(request.eventId()));
        if (!bookingPolicy.isBookable(event)) {
            throw new BusinessRuleException("Booking is closed for this event.");
        }

        Instant now = bookingPolicy.now();
        List<Seat> seats = seatLockService.acquire(strategy, event.getId(), request.seatIds());
        List<String> unavailable = seats.stream()
                .filter(seat -> !seat.isAvailableTo(userId, now))
                .map(Seat::getSeatNumber)
                .toList();
        if (!unavailable.isEmpty()) {
            throw new SeatAlreadyBookedException(unavailable);
        }

        seats.forEach(Seat::book);
        seatLockService.flushSeatChanges();

        Booking booking = Booking.confirm(referenceGenerator.next(), userRepository.getReferenceById(userId), event, seats, now);
        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new BookingConflictException(BookingConflictException.SEATS_UNAVAILABLE, ex);
        }

        eventPublisher.publishEvent(SeatsChangedEvent.of(event.getId(), booking.seatIds()));
        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(Long userId, int page, int size) {
        Page<Booking> bookings = bookingRepository.findByUserIdOrderByBookingTimeDesc(userId, PageRequest.of(page, size));
        return PageResponse.of(bookings, bookingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(AuthenticatedUser viewer, Long bookingId) {
        Booking booking = findAccessibleBooking(viewer, bookingId);
        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(AuthenticatedUser viewer, Long bookingId) {
        Booking booking = findAccessibleBooking(viewer, bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("This booking is already cancelled.");
        }
        if (!bookingPolicy.isCancellable(booking)) {
            throw new BusinessRuleException("Bookings can only be cancelled up to "
                    + bookingPolicy.cancellationCutoffHours() + " hours before the event starts.");
        }

        booking.cancel(bookingPolicy.now());
        try {
            bookingRepository.flush();
        } catch (OptimisticLockingFailureException ex) {
            throw new BookingConflictException("This booking was just updated. Please refresh and try again.", ex);
        }

        eventPublisher.publishEvent(SeatsChangedEvent.of(booking.getEvent().getId(), booking.seatIds()));
        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminBookingResponse> searchBookings(Long eventId, BookingStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bookingTime", "id"));
        Page<Booking> bookings = bookingRepository.findAll(BookingSpecifications.matching(eventId, status), pageable);
        return PageResponse.of(bookings, bookingMapper::toAdminResponse);
    }

    private Booking findAccessibleBooking(AuthenticatedUser viewer, Long bookingId) {
        Booking booking = bookingRepository.findWithDetailsById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.booking(bookingId));
        if (!booking.isOwnedBy(viewer.id()) && !viewer.isAdmin()) {
            throw new AccessDeniedException("You can only access your own bookings.");
        }
        return booking;
    }
}
