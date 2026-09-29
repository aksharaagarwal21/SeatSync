package com.seatsync.dto.admin;

import com.seatsync.entity.Role;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt,
        Long confirmedBookings
) {
}
