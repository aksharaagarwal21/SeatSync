package com.seatsync.exception;

/** A concurrent transaction changed the same rows first (optimistic version mismatch or unique-index hit). */
public class BookingConflictException extends RuntimeException {

    public static final String SEATS_UNAVAILABLE = "One or more selected seats are no longer available.";

    public BookingConflictException(String message) {
        super(message);
    }

    public BookingConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
