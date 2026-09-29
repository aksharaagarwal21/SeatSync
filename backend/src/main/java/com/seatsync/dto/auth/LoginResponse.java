package com.seatsync.dto.auth;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, UserResponse user) {
}
