package com.seatsync.exception;

import com.seatsync.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String BOOKING_CONFLICT = "Booking Conflict";

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "Not Found", "The requested resource does not exist.");
    }

    @ExceptionHandler(SeatAlreadyBookedException.class)
    ResponseEntity<ApiError> handleSeatAlreadyBooked(SeatAlreadyBookedException ex) {
        return respond(HttpStatus.CONFLICT, BOOKING_CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BookingConflictException.class)
    ResponseEntity<ApiError> handleBookingConflict(BookingConflictException ex) {
        return respond(HttpStatus.CONFLICT, BOOKING_CONFLICT, ex.getMessage());
    }

    /** Safety net for version conflicts or lock failures that surface at commit time. */
    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    ResponseEntity<ApiError> handleLockingFailure(RuntimeException ex) {
        log.debug("Locking conflict: {}", ex.getMessage());
        return respond(HttpStatus.CONFLICT, BOOKING_CONFLICT, BookingConflictException.SEATS_UNAVAILABLE);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Integrity constraint rejected a write: {}", ex.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, "Conflict", "The request conflicts with existing data. Please refresh and try again.");
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ApiError> handleEmailTaken(EmailAlreadyRegisteredException ex) {
        return respond(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "Request Not Allowed", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiError.validation(fieldErrors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex) {
        String message = ex.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request parameters.");
        return respond(HttpStatus.BAD_REQUEST, "Validation Failed", message);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    ResponseEntity<ApiError> handleMalformedRequest(Exception ex) {
        return respond(HttpStatus.BAD_REQUEST, "Bad Request", "The request is malformed or has invalid parameters.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return respond(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password.");
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return respond(HttpStatus.UNAUTHORIZED, "Unauthorized", "Please sign in to continue.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return respond(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "We couldn't process your request. Please try again in a moment.");
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status, error, message));
    }
}
