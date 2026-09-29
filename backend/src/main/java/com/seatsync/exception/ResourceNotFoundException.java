package com.seatsync.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException event(Long id) {
        return new ResourceNotFoundException("Event " + id + " was not found.");
    }

    public static ResourceNotFoundException booking(Long id) {
        return new ResourceNotFoundException("Booking " + id + " was not found.");
    }
}
