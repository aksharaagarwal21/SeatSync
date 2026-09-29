package com.seatsync.security;

import com.seatsync.config.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * The browser keeps the JWT in an HttpOnly cookie so scripts can never read it (XSS-safe).
 * SameSite=Lax stops the cookie from being sent on cross-site POST/PUT/DELETE, which is what
 * lets the API run with CSRF tokens disabled.
 */
@Component
public class AuthCookieFactory {

    private static final String COOKIE_PATH = "/api";

    private final JwtProperties properties;
    private final Clock clock;

    public AuthCookieFactory(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public ResponseCookie create(String token, Instant expiresAt) {
        return base(token).maxAge(Duration.between(clock.instant(), expiresAt)).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Lax")
                .path(COOKIE_PATH);
    }
}
