package com.seatsync.entity;

public enum OtpPurpose {
    LOGIN,
    REGISTRATION,
    BOOKING,
    CANCELLATION;

    public boolean isSignIn() {
        return this == LOGIN || this == REGISTRATION;
    }
}
