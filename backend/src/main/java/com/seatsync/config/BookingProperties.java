package com.seatsync.config;

import com.seatsync.service.locking.LockingStrategy;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @param lockingStrategy    how seats are locked during holds and bookings (benchmarkable via env var)
 * @param holdDuration       how long seats stay RESERVED for a user while they review the booking
 * @param cancellationCutoff bookings can be cancelled until this long before the event starts
 */
@Validated
@ConfigurationProperties(prefix = "seatsync.booking")
public record BookingProperties(
        @NotNull LockingStrategy lockingStrategy,
        @NotNull Duration holdDuration,
        @NotNull Duration cancellationCutoff
) {
}
