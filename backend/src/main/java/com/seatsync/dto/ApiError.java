package com.seatsync.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {

    public static ApiError of(HttpStatus status, String error, String message) {
        return new ApiError(Instant.now(), status.value(), error, message, null);
    }

    public static ApiError validation(Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Validation Failed",
                "Please correct the highlighted fields.", fieldErrors);
    }
}
