package com.seatsync.service.verification;

import com.seatsync.config.OtpProperties;
import com.seatsync.dto.verification.ActionVerificationRequest;
import com.seatsync.dto.verification.VerificationChallengeResponse;
import com.seatsync.entity.Booking;
import com.seatsync.entity.Event;
import com.seatsync.entity.OtpPurpose;
import com.seatsync.entity.Seat;
import com.seatsync.entity.User;
import com.seatsync.exception.BusinessRuleException;
import com.seatsync.exception.ResourceNotFoundException;
import com.seatsync.repository.BookingRepository;
import com.seatsync.repository.EventRepository;
import com.seatsync.repository.SeatRepository;
import com.seatsync.repository.UserRepository;
import com.seatsync.service.BookingPolicy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Issues codes for sensitive actions. Each code is bound to the exact action (these seats, this
 * booking), and the email tells the user what they are approving.
 */
@Service
public class VerificationService {

    private final OtpService otpService;
    private final OtpProperties properties;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final BookingPolicy bookingPolicy;

    public VerificationService(OtpService otpService,
                               OtpProperties properties,
                               UserRepository userRepository,
                               EventRepository eventRepository,
                               SeatRepository seatRepository,
                               BookingRepository bookingRepository,
                               BookingPolicy bookingPolicy) {
        this.otpService = otpService;
        this.properties = properties;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.bookingPolicy = bookingPolicy;
    }

    @Transactional
    public VerificationChallengeResponse requestActionCode(Long userId, boolean isAdmin, ActionVerificationRequest request) {
        if (!properties.enabled()) {
            return VerificationChallengeResponse.notRequired();
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Your account no longer exists."));
        return switch (request.purpose()) {
            case BOOKING -> requestBookingCode(user, request.eventId(), request.seatIds());
            case CANCELLATION -> requestCancellationCode(user, isAdmin, request.bookingId());
            case LOGIN, REGISTRATION -> throw new BusinessRuleException("Sign-in codes are sent when you sign in.");
        };
    }

    private VerificationChallengeResponse requestBookingCode(User user, Long eventId, List<Long> seatIds) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> ResourceNotFoundException.event(eventId));
        if (!bookingPolicy.isBookable(event)) {
            throw new BusinessRuleException("Booking is closed for this event.");
        }
        List<Seat> seats = seatRepository.findForBooking(eventId, seatIds.stream().distinct().sorted().toList());
        if (seats.size() != seatIds.stream().distinct().count()) {
            throw new ResourceNotFoundException("One or more selected seats do not exist for this event.");
        }
        String seatNumbers = String.join(", ", seats.stream().map(Seat::getSeatNumber).toList());
        String summary = "Book %s for %s (%s)".formatted(seatNumbers, event.getName(), rupees(Booking.calculateTotal(seats)));
        return otpService.issue(user, OtpPurpose.BOOKING, ActionVerification.bookingContext(eventId, seatIds), truncate(summary));
    }

    private VerificationChallengeResponse requestCancellationCode(User user, boolean isAdmin, Long bookingId) {
        Booking booking = bookingRepository.findWithDetailsById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.booking(bookingId));
        if (!booking.isOwnedBy(user.getId()) && !isAdmin) {
            throw new AccessDeniedException("You can only cancel your own bookings.");
        }
        if (!bookingPolicy.isCancellable(booking)) {
            throw new BusinessRuleException("This booking can no longer be cancelled.");
        }
        String summary = "Cancel booking %s for %s (%s)".formatted(
                booking.getReference(), booking.getEvent().getName(), rupees(booking.getTotalAmount()));
        return otpService.issue(user, OtpPurpose.CANCELLATION, ActionVerification.cancellationContext(bookingId), truncate(summary));
    }

    private static String rupees(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"));
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }

    private static String truncate(String summary) {
        return summary.length() <= 300 ? summary : summary.substring(0, 297) + "...";
    }
}
