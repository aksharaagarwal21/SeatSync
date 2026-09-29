package com.seatsync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param enabled        seed demo events, seats and bookings into an empty database
 * @param adminEmail     email of the seeded admin; must be a real inbox when two-step verification is on
 * @param adminPassword  required whenever seeding runs; the dev profile supplies a local-only default
 * @param loadTestUsers  also create the 500 accounts the JMeter plan signs in with
 */
@ConfigurationProperties(prefix = "seatsync.seed")
public record SeedProperties(boolean enabled, String adminEmail, String adminPassword, boolean loadTestUsers) {
}
