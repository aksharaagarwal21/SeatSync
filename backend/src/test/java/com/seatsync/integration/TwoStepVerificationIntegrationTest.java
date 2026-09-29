package com.seatsync.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.seatsync.dto.admin.AdminEventResponse;
import com.seatsync.dto.verification.VerificationCode;
import com.seatsync.entity.Role;
import com.seatsync.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every rule of the one-time codes: attempts, expiry, single use, binding, ownership, rate limits. */
class TwoStepVerificationIntegrationTest extends AbstractIntegrationTest {

    private AdminEventResponse event;
    private List<Long> seats;
    private String alice;
    private String bob;

    @BeforeEach
    void setUp() throws Exception {
        event = createEvent(1, 10);
        seats = seatIds(event.id());
        createUser("alice@test.dev", Role.USER);
        createUser("bob@test.dev", Role.USER);
        alice = login("alice@test.dev");
        bob = login("bob@test.dev");
    }

    @Test
    void wrongCodesCountDownAndLockTheChallenge() throws Exception {
        UUID challengeId = startLogin("alice@test.dev");
        String correct = otpNotifier.lastCodeFor("alice@test.dev");
        String wrong = correct.equals("000000") ? "111111" : "000000";

        for (int remaining = 4; remaining >= 1; remaining--) {
            verifyLogin(challengeId, wrong)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("That code is incorrect. " + remaining + (remaining == 1 ? " attempt" : " attempts") + " left."));
        }
        verifyLogin(challengeId, wrong).andExpect(jsonPath("$.message").value("Too many incorrect attempts. Request a new code."));
        verifyLogin(challengeId, correct)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Too many incorrect attempts. Request a new code."));
    }

    @Test
    void aCodeWorksOnlyOnce() throws Exception {
        UUID challengeId = startLogin("alice@test.dev");
        String code = otpNotifier.lastCodeFor("alice@test.dev");

        verifyLogin(challengeId, code).andExpect(status().isOk());
        verifyLogin(challengeId, code)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("This code has already been used. Request a new code."));
    }

    @Test
    void expiredCodesAreRejected() throws Exception {
        UUID challengeId = startLogin("alice@test.dev");
        jdbc.update("UPDATE otp_challenges SET expires_at = now() - interval '1 second' WHERE id = ?", challengeId);

        verifyLogin(challengeId, otpNotifier.lastCodeFor("alice@test.dev"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("This code has expired. Request a new code."));
    }

    @Test
    void codesAreStoredOnlyAsKeyedHashes() throws Exception {
        UUID challengeId = startLogin("alice@test.dev");
        String stored = jdbc.queryForObject("SELECT code_hash FROM otp_challenges WHERE id = ?", String.class, challengeId);

        assertThat(stored).hasSize(64).doesNotContain(otpNotifier.lastCodeFor("alice@test.dev"));
    }

    @Test
    void bookingWithoutACodeIsRejected() throws Exception {
        mockMvc.perform(post("/api/bookings").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", event.id(), "seatIds", List.of(seats.getFirst())))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Enter the 6-digit code we emailed you to continue."));
        assertThat(seatStatus(seats.getFirst())).isEqualTo("AVAILABLE");
    }

    @Test
    void aBookingCodeOnlyApprovesTheSeatsItWasIssuedFor() throws Exception {
        VerificationCode codeForA1 = requestCode(alice, bookingCodeRequest(List.of(seats.get(0))));
        assertThat(otpNotifier.lastSummaryFor("alice@test.dev")).startsWith("Book A1 for Concurrency Night");

        book(alice, List.of(seats.get(1)), codeForA1)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("This code was issued for a different action. Request a new code."));
        book(alice, List.of(seats.get(0)), codeForA1).andExpect(status().isCreated());
    }

    @Test
    void anotherUsersCodeIsUseless() throws Exception {
        VerificationCode alicesCode = requestCode(alice, bookingCodeRequest(List.of(seats.get(0))));

        book(bob, List.of(seats.get(0)), alicesCode)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", startsWith("This verification code is invalid")));
    }

    @Test
    void losingTheSeatRaceDoesNotBurnTheCode() throws Exception {
        VerificationCode alicesCode = requestCode(alice, bookingCodeRequest(List.of(seats.get(0))));
        mockMvc.perform(verifiedBooking(bob, event.id(), List.of(seats.get(0)))).andExpect(status().isCreated());

        book(alice, List.of(seats.get(0)), alicesCode).andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT consumed_at IS NULL FROM otp_challenges WHERE id = ?", Boolean.class, alicesCode.challengeId()))
                .as("the booking rolled back, so consuming the code rolled back with it")
                .isTrue();
    }

    @Test
    void cancellationNeedsItsOwnCode() throws Exception {
        String body = mockMvc.perform(verifiedBooking(alice, event.id(), List.of(seats.get(2))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long bookingId = readJson(body).get("id").asLong();

        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId).header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(otpNotifier.lastSummaryFor("alice@test.dev")).startsWith("Book A3");

        mockMvc.perform(verifiedCancellation(alice, bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(otpNotifier.lastSummaryFor("alice@test.dev")).startsWith("Cancel booking SS-");
    }

    @Test
    void resendRespectsTheCooldownAndReplacesTheCode() throws Exception {
        UUID challengeId = startLogin("alice@test.dev");
        String firstCode = otpNotifier.lastCodeFor("alice@test.dev");

        mockMvc.perform(post("/api/verifications/{id}/resend", challengeId))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", startsWith("You can request a new code in")));

        jdbc.update("UPDATE otp_challenges SET last_sent_at = now() - interval '1 minute' WHERE id = ?", challengeId);
        mockMvc.perform(post("/api/verifications/{id}/resend", challengeId)).andExpect(status().isOk());
        String secondCode = otpNotifier.lastCodeFor("alice@test.dev");

        if (!secondCode.equals(firstCode)) {
            verifyLogin(challengeId, firstCode).andExpect(status().isUnprocessableEntity());
        }
        verifyLogin(challengeId, secondCode).andExpect(status().isOk());
    }

    @Test
    void requestingTooManyCodesIsRateLimited() throws Exception {
        // setUp already issued one sign-in code for alice; the limit is 10 per 15 minutes.
        for (int i = 0; i < 9; i++) {
            startLogin("alice@test.dev");
        }
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "alice@test.dev", "password", PASSWORD))))
                .andExpect(status().isTooManyRequests());
    }

    private UUID startLogin(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode verification = readJson(body).get("verification");
        return UUID.fromString(verification.get("challengeId").asText());
    }

    private org.springframework.test.web.servlet.ResultActions verifyLogin(UUID challengeId, String code) throws Exception {
        return mockMvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                .content(json(new VerificationCode(challengeId, code))));
    }

    private Map<String, Object> bookingCodeRequest(List<Long> seatIds) {
        return Map.of("purpose", "BOOKING", "eventId", event.id(), "seatIds", seatIds);
    }

    private org.springframework.test.web.servlet.ResultActions book(String token, List<Long> seatIds, VerificationCode code) throws Exception {
        return mockMvc.perform(post("/api/bookings").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("eventId", event.id(), "seatIds", seatIds, "verification", code))));
    }

    private String seatStatus(Long seatId) {
        return jdbc.queryForObject("SELECT status FROM seats WHERE id = ?", String.class, seatId);
    }
}
