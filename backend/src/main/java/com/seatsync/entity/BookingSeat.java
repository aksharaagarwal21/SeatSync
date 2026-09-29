package com.seatsync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_seats")
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Backed by a partial unique index: one active booking line per seat. */
    @Column(nullable = false)
    private boolean active;

    protected BookingSeat() {
    }

    BookingSeat(Booking booking, Seat seat, BigDecimal price) {
        this.booking = booking;
        this.seat = seat;
        this.price = price;
        this.active = true;
    }

    void release() {
        this.active = false;
        seat.release();
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public Seat getSeat() {
        return seat;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }
}
