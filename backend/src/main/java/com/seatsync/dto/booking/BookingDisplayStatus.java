package com.seatsync.dto.booking;

/** What the customer sees: a confirmed booking whose event has started is shown as COMPLETED. */
public enum BookingDisplayStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
