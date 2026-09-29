package com.seatsync.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "seatsync.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT_SECRET must be set")
        @Size(min = 32, message = "JWT_SECRET must be at least 32 characters")
        String secret,
        @NotNull Duration expiration,
        @NotBlank String cookieName,
        boolean cookieSecure
) {
}
