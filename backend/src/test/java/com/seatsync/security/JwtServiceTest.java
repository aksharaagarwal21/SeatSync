package com.seatsync.security;

import com.seatsync.config.JwtProperties;
import com.seatsync.entity.Role;
import com.seatsync.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private final JwtProperties properties = new JwtProperties(
            "unit-test-secret-that-is-long-enough-for-hs256", Duration.ofHours(1), "seatsync_token", false);

    @Test
    void issuedTokenRoundTripsUserClaims() {
        JwtService service = new JwtService(properties, Clock.fixed(NOW, ZoneOffset.UTC));

        JwtService.IssuedToken token = service.issue(TestFixtures.user(42, Role.ADMIN));

        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(service.parse(token.value())).hasValueSatisfying(user -> {
            assertThat(user.id()).isEqualTo(42L);
            assertThat(user.role()).isEqualTo(Role.ADMIN);
            assertThat(user.isAdmin()).isTrue();
        });
    }

    @Test
    void expiredTokenIsRejected() {
        String token = new JwtService(properties, Clock.fixed(NOW, ZoneOffset.UTC))
                .issue(TestFixtures.user(1, Role.USER)).value();
        JwtService later = new JwtService(properties, Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC));

        assertThat(later.parse(token)).isEmpty();
    }

    @Test
    void tamperedOrForeignTokensAreRejected() {
        JwtService service = new JwtService(properties, Clock.fixed(NOW, ZoneOffset.UTC));
        String token = service.issue(TestFixtures.user(1, Role.USER)).value();
        JwtService otherKey = new JwtService(new JwtProperties(
                "a-completely-different-secret-for-signing-tokens", Duration.ofHours(1), "seatsync_token", false),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.parse(token.substring(0, token.length() - 2) + "xx")).isEmpty();
        assertThat(otherKey.parse(token)).isEmpty();
        assertThat(service.parse("not-a-jwt")).isEmpty();
    }
}
