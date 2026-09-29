package com.seatsync.exception;

import java.util.List;

/** One or more requested seats were already booked or held by someone else when we locked them. */
public class SeatAlreadyBookedException extends RuntimeException {

    private final List<String> seatNumbers;

    public SeatAlreadyBookedException(List<String> seatNumbers) {
        super(buildMessage(seatNumbers));
        this.seatNumbers = List.copyOf(seatNumbers);
    }

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    private static String buildMessage(List<String> seatNumbers) {
        return seatNumbers.size() == 1
                ? "Seat " + seatNumbers.getFirst() + " is no longer available."
                : "Seats " + String.join(", ", seatNumbers) + " are no longer available.";
    }
}
