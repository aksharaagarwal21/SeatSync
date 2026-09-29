package com.seatsync.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "booking_time", nullable = false, updatable = false)
    private Instant bookingTime;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<BookingSeat> seats = new ArrayList<>();

    protected Booking() {
    }

    private Booking(String reference, User user, Event event, Instant bookingTime) {
        this.reference = reference;
        this.user = user;
        this.event = event;
        this.bookingTime = bookingTime;
        this.status = BookingStatus.CONFIRMED;
    }

    /**
     * Creates a confirmed booking for seats the caller has already locked and marked BOOKED.
     * Each seat's current price is captured on the booking line so later price changes
     * never alter what the customer paid.
     */
    public static Booking confirm(String reference, User user, Event event, Collection<Seat> seats, Instant bookingTime) {
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("A booking needs at least one seat");
        }
        if (seats.stream().anyMatch(seat -> seat.getStatus() != SeatStatus.BOOKED)) {
            throw new IllegalStateException("Seats must be marked BOOKED before the booking is confirmed");
        }
        Booking booking = new Booking(reference, user, event, bookingTime);
        seats.forEach(seat -> booking.seats.add(new BookingSeat(booking, seat, seat.getPrice())));
        booking.totalAmount = calculateTotal(seats);
        return booking;
    }

    public static BigDecimal calculateTotal(Collection<Seat> seats) {
        return seats.stream().map(Seat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void cancel(Instant now) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = now;
        seats.forEach(BookingSeat::release);
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    public List<Long> seatIds() {
        return seats.stream().map(line -> line.getSeat().getId()).toList();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public User getUser() {
        return user;
    }

    public Event getEvent() {
        return event;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getBookingTime() {
        return bookingTime;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public List<BookingSeat> getSeats() {
        return seats;
    }
}
