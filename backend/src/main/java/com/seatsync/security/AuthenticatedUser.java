package com.seatsync.security;

import com.seatsync.entity.Role;

/** The principal placed in the SecurityContext, rebuilt from JWT claims without a database hit. */
public record AuthenticatedUser(Long id, String email, String name, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
