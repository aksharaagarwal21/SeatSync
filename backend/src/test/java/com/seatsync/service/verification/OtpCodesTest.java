package com.seatsync.service.verification;

import com.seatsync.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OtpCodesTest {

    private final OtpCodes codes = new OtpCodes(
            new JwtProperties("unit-test-secret-that-is-long-enough-for-hs256", Duration.ofHours(1), "seatsync_token", false));

    @Test
    void codesAreSixDigitsIncludingLeadingZeros() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 2_000; i++) {
            String code = codes.generate();
            assertThat(code).matches("\\d{6}");
            seen.add(code);
        }
        assertThat(seen).hasSizeGreaterThan(1_990);
    }

    @Test
    void hashIsKeyedAndOnlyMatchesTheSameCode() {
        String hash = codes.hash("042917");

        assertThat(hash).hasSize(64).isNotEqualTo("042917");
        assertThat(codes.matches("042917", hash)).isTrue();
        assertThat(codes.matches("042918", hash)).isFalse();

        OtpCodes otherKey = new OtpCodes(
                new JwtProperties("a-completely-different-secret-for-the-hmac-key", Duration.ofHours(1), "seatsync_token", false));
        assertThat(otherKey.hash("042917")).isNotEqualTo(hash);
    }

    @Test
    void emailsAreMaskedForDisplay() {
        assertThat(OtpCodes.maskEmail("priya.sharma@gmail.com")).isEqualTo("p***a@gmail.com");
        assertThat(OtpCodes.maskEmail("a@b.co")).isEqualTo("***@b.co");
    }
}
