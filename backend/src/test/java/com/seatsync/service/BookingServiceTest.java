package com.seatsync.service;

import com.seatsync.config.BookingProperties;
import com.seatsync.dto.booking.BookingDisplayStatus;
import com.seatsync.dto.booking.BookingResponse;
import com.seatsync.dto.booking.CreateBookingRequest;
import com.seatsync.entity.Booking;
import com.seatsync.entity.Event;
import com.seatsync.entity.EventStatus;
import com.seatsync.entity.Role;
import com.seatsync.entity.Seat;
import com.seatsync.entity.SeatStatus;
import com.seatsync.entity.User;
import com.seatsync.exception.BookingConflictException;
import com.seatsync.exception.BusinessRuleException;
import com.seatsync.exception.ResourceNotFoundException;
import com.seatsync.exception.SeatAlreadyBookedException;
import com.seatsync.mapper.BookingMapper;
import com.seatsync.repository.BookingRepository;
import com.seatsync.repository.EventRepository;
import com.seatsync.repository.UserRepository;
import com.seatsync.security.AuthenticatedUser;
import com.seatsync.service.locking.LockingStrategy;
import com.seatsync.service.locking.SeatLockService;
import com.seatsync.service.realtime.SeatsChangedEvent;
import com.seatsync.service.verification.ActionVerification;
import com.seatsync.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final Instant NOW = Instant.parse("2026-09-01T06:30:00Z");
    private static final Long USER_ID = 3L;

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private SeatLockService seatLockService;
    @Mock private BookingReferenceGenerator referenceGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ActionVerification actionVerification;

    private BookingService bookingService;
    private Event event;
    private User user;

    @BeforeEach
    void setUp() {
        var properties = new BookingProperties(LockingStrategy.OPTIMISTIC, Duration.ofMinutes(5), Duration.ofHours(24));
        var policy = new BookingPolicy(Clock.fixed(NOW, ZONE), properties);
        bookingService = new BookingService(eventRepository, userRepository, bookingRepository, seatLockService,
                policy, referenceGenerator, new BookingMapper(policy), eventPublisher, properties, actionVerification);
        event = TestFixtures.event(10, LocalDate.of(2026, 9, 20), EventStatus.ON_SALE);
        user = TestFixtures.user(USER_ID, Role.USER);
    }

    @Nested
    class CreateBooking {

        @Test
        void booksAllSeatsAndCalculatesTotal() {
            Seat a10 = TestFixtures.seat(1, event, "A", 10, "499.00");
            Seat a11 = TestFixtures.seat(2, event, "A", 11, "499.00");
            givenBookableEvent(a10, a11);

            BookingResponse response = bookingService.createBooking(USER_ID, request(1L, 2L));

            assertThat(response.status()).isEqualTo(BookingDisplayStatus.CONFIRMED);
            assertThat(response.reference()).isEqualTo("SS-TEST0001");
            assertThat(response.totalAmount()).isEqualByComparingTo("998.00");
            assertThat(response.seats()).extracting("seatNumber").containsExactly("A10", "A11");
            assertThat(response.cancellable()).isTrue();
            assertThat(a10.getStatus()).isEqualTo(SeatStatus.BOOKED);
            assertThat(a11.getStatus()).isEqualTo(SeatStatus.BOOKED);
            verify(seatLockService).flushSeatChanges();
            verify(bookingRepository).saveAndFlush(any(Booking.class));
            verify(actionVerification).require(eq(USER_ID), eq(com.seatsync.entity.OtpPurpose.BOOKING), eq("event:10|seats:1,2"), any());
        }

        @Test
        void publishesSeatChangeForRealTimeClients() {
            Seat seat = TestFixtures.seat(1, event, "A", 10, "499.00");
            givenBookableEvent(seat);

            bookingService.createBooking(USER_ID, request(1L));

            ArgumentCaptor<SeatsChangedEvent> captor = ArgumentCaptor.forClass(SeatsChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().eventId()).isEqualTo(10L);
            assertThat(captor.getValue().seatIds()).containsExactly(1L);
        }

        @Test
        void usesTheRequestedLockingStrategy() {
            Seat seat = TestFixtures.seat(1, event, "A", 10, "499.00");
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(seatLockService.acquire(eq(LockingStrategy.PESSIMISTIC), eq(10L), anyCollection())).thenReturn(List.of(seat));
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
            when(referenceGenerator.next()).thenReturn("SS-TEST0001");

            bookingService.createBooking(USER_ID, request(1L), LockingStrategy.PESSIMISTIC);

            verify(seatLockService).acquire(eq(LockingStrategy.PESSIMISTIC), eq(10L), anyCollection());
        }

        @Test
        void rejectsWholeBookingWhenAnySeatIsTaken() {
            Seat free = TestFixtures.seat(1, event, "A", 10, "499.00");
            Seat taken = TestFixtures.seat(2, event, "A", 11, "499.00");
            taken.book();
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(seatLockService.acquire(any(), eq(10L), anyCollection())).thenReturn(List.of(free, taken));

            assertThatThrownBy(() -> bookingService.createBooking(USER_ID, request(1L, 2L)))
                    .isInstanceOf(SeatAlreadyBookedException.class)
                    .hasMessage("Seat A11 is no longer available.");

            assertThat(free.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            verify(bookingRepository, never()).saveAndFlush(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void rejectsSeatHeldByAnotherUser() {
            Seat held = TestFixtures.seat(1, event, "A", 10, "499.00");
            held.hold(99L, NOW.plusSeconds(120));
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(seatLockService.acquire(any(), eq(10L), anyCollection())).thenReturn(List.of(held));

            assertThatThrownBy(() -> bookingService.createBooking(USER_ID, request(1L)))
                    .isInstanceOf(SeatAlreadyBookedException.class);
        }

        @Test
        void allowsBookingSeatsTheUserIsHolding() {
            Seat held = TestFixtures.seat(1, event, "A", 10, "499.00");
            held.hold(USER_ID, NOW.plusSeconds(120));
            givenBookableEvent(held);

            BookingResponse response = bookingService.createBooking(USER_ID, request(1L));

            assertThat(response.status()).isEqualTo(BookingDisplayStatus.CONFIRMED);
            assertThat(held.getHeldByUserId()).isNull();
        }

        @Test
        void translatesVersionConflictIntoBookingConflict() {
            Seat seat = TestFixtures.seat(1, event, "A", 10, "499.00");
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(seatLockService.acquire(any(), eq(10L), anyCollection())).thenReturn(List.of(seat));
            doThrow(new BookingConflictException(BookingConflictException.SEATS_UNAVAILABLE))
                    .when(seatLockService).flushSeatChanges();

            assertThatThrownBy(() -> bookingService.createBooking(USER_ID, request(1L)))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("One or more selected seats are no longer available.");
            verify(bookingRepository, never()).saveAndFlush(any());
        }

        @Test
        void failsForUnknownEvent() {
            when(eventRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.createBooking(USER_ID, request(1L)))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(seatLockService, never()).acquire(any(), any(), anyCollection());
        }

        @Test
        void failsWhenSalesArePaused() {
            Event paused = TestFixtures.event(10, LocalDate.of(2026, 9, 20), EventStatus.PAUSED);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(paused));

            assertThatThrownBy(() -> bookingService.createBooking(USER_ID, request(1L)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Booking is closed for this event.");
        }

        @Test
        void failsWhenEventHasAlreadyStarted() {
            Event past = TestFixtures.event(10, LocalDate.of(2026, 8, 30), EventStatus.ON_SALE);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(past));

            assertThatThrownBy(() -> bookingService.createBooking(USER_ID, request(1L)))
                    .isInstanceOf(BusinessRuleException.class);
        }

        private void givenBookableEvent(Seat... seats) {
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(seatLockService.acquire(eq(LockingStrategy.OPTIMISTIC), eq(10L), anyCollection())).thenReturn(List.of(seats));
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
            when(referenceGenerator.next()).thenReturn("SS-TEST0001");
        }
    }

    @Nested
    class CancelBooking {

        private final AuthenticatedUser owner = new AuthenticatedUser(USER_ID, "owner@test.dev", "Owner", Role.USER);

        @Test
        void ownerCanCancelAndSeatsBecomeAvailable() {
            Seat seat = TestFixtures.seat(1, event, "A", 10, "499.00");
            Booking booking = confirmedBooking(event, seat);
            when(bookingRepository.findWithDetailsById(50L)).thenReturn(Optional.of(booking));

            BookingResponse response = bookingService.cancelBooking(owner, 50L, null);

            assertThat(response.status()).isEqualTo(BookingDisplayStatus.CANCELLED);
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            verify(eventPublisher).publishEvent(any(SeatsChangedEvent.class));
        }

        @Test
        void otherUsersCannotCancel() {
            Booking booking = confirmedBooking(event, TestFixtures.seat(1, event, "A", 10, "499.00"));
            when(bookingRepository.findWithDetailsById(50L)).thenReturn(Optional.of(booking));
            var stranger = new AuthenticatedUser(77L, "x@test.dev", "Stranger", Role.USER);

            assertThatThrownBy(() -> bookingService.cancelBooking(stranger, 50L, null))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void cannotCancelWithinCutoffBeforeEvent() {
            Event tomorrowMorning = TestFixtures.event(10, LocalDate.of(2026, 9, 1), EventStatus.ON_SALE);
            Booking booking = confirmedBooking(tomorrowMorning, TestFixtures.seat(1, tomorrowMorning, "A", 10, "499.00"));
            when(bookingRepository.findWithDetailsById(50L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(owner, 50L, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("24 hours");
        }

        @Test
        void cannotCancelTwice() {
            Booking booking = confirmedBooking(event, TestFixtures.seat(1, event, "A", 10, "499.00"));
            booking.cancel(NOW);
            when(bookingRepository.findWithDetailsById(50L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(owner, 50L, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("This booking is already cancelled.");
        }

        private Booking confirmedBooking(Event bookedEvent, Seat seat) {
            seat.book();
            return Booking.confirm("SS-TEST0001", user, bookedEvent, List.of(seat), NOW.minusSeconds(3600));
        }
    }

    private static CreateBookingRequest request(Long... seatIds) {
        return new CreateBookingRequest(10L, List.of(seatIds));
    }
}
