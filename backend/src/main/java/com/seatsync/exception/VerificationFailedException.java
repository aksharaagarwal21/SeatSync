package com.seatsync.exception;

/** A verification code was missing, wrong, expired, already used or issued for another action. */
public class VerificationFailedException extends RuntimeException {

    public VerificationFailedException(String message) {
        super(message);
    }
}
