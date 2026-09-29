package com.seatsync.dto.auth;

import com.seatsync.entity.Role;

public record UserResponse(Long id, String name, String email, Role role) {
}
