package com.seatsync.controller;

import com.seatsync.dto.PageResponse;
import com.seatsync.dto.booking.BookingResponse;
import com.seatsync.dto.booking.CancelBookingRequest;
import com.seatsync.dto.booking.CreateBookingRequest;
import com.seatsync.security.AuthenticatedUser;
import com.seatsync.service.BookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/bookings")
@Validated
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse booking = bookingService.createBooking(user.id(), request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(booking.id()).toUri();
        return ResponseEntity.created(location).body(booking);
    }

    @GetMapping("/me")
    public PageResponse<BookingResponse> getMyBookings(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                                       @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return bookingService.getMyBookings(user.id(), page, size);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBooking(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long bookingId) {
        return bookingService.getBooking(user, bookingId);
    }

    @PostMapping("/{bookingId}/cancel")
    public BookingResponse cancelBooking(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable Long bookingId,
                                         @Valid @RequestBody CancelBookingRequest request) {
        return bookingService.cancelBooking(user, bookingId, request.verification());
    }
}
