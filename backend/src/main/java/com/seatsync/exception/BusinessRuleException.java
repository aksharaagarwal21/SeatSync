package com.seatsync.exception;

/** The request is well-formed but not allowed in the current state (e.g. booking closed, cancellation window passed). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
