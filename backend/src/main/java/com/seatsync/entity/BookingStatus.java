package com.seatsync.entity;

/**
 * PENDING is part of the schema for a future payment step; the current flow holds seats
 * (Seat.RESERVED) instead and creates bookings directly as CONFIRMED.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
