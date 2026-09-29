package com.seatsync.mapper;

import com.seatsync.dto.admin.AdminBookingResponse;
import com.seatsync.dto.booking.BookedSeatResponse;
import com.seatsync.dto.booking.BookingResponse;
import com.seatsync.entity.Booking;
import com.seatsync.entity.BookingSeat;
import com.seatsync.entity.Event;
import com.seatsync.entity.Seat;
import com.seatsync.service.BookingPolicy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {

    private final BookingPolicy bookingPolicy;

    public BookingMapper(BookingPolicy bookingPolicy) {
        this.bookingPolicy = bookingPolicy;
    }

    public BookingResponse toResponse(Booking booking) {
        Event event = booking.getEvent();
        List<BookedSeatResponse> seats = booking.getSeats().stream()
                .map(BookingMapper::toBookedSeat)
                .toList();
        return new BookingResponse(
                booking.getId(),
                booking.getReference(),
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getCity(),
                event.getEventDate(),
                event.getStartTime(),
                event.getImageUrl(),
                seats,
                booking.getTotalAmount(),
                bookingPolicy.displayStatus(booking),
                booking.getBookingTime(),
                bookingPolicy.isCancellable(booking));
    }

    public AdminBookingResponse toAdminResponse(Booking booking) {
        Event event = booking.getEvent();
        List<String> seatNumbers = booking.getSeats().stream()
                .map(line -> line.getSeat().getSeatNumber())
                .toList();
        return new AdminBookingResponse(
                booking.getId(),
                booking.getReference(),
                booking.getUser().getName(),
                booking.getUser().getEmail(),
                event.getId(),
                event.getName(),
                event.getEventDate(),
                seatNumbers,
                booking.getTotalAmount(),
                bookingPolicy.displayStatus(booking),
                booking.getBookingTime());
    }

    private static BookedSeatResponse toBookedSeat(BookingSeat line) {
        Seat seat = line.getSeat();
        return new BookedSeatResponse(seat.getId(), seat.getSeatNumber(), seat.getSection(), line.getPrice());
    }
}
