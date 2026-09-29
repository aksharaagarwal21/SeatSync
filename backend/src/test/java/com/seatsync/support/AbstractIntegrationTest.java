package com.seatsync.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatsync.dto.admin.AdminEventResponse;
import com.seatsync.dto.admin.EventRequest;
import com.seatsync.dto.admin.SectionPricing;
import com.seatsync.dto.verification.VerificationCode;
import com.seatsync.entity.EventCategory;
import com.seatsync.entity.Role;
import com.seatsync.service.AdminEventService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full application context against PostgreSQL in Docker, with two-step verification on.
 * Codes are read from {@link RecordingOtpNotifier} instead of email. Every test starts from empty tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Password123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected AdminEventService adminEventService;
    @Autowired protected RecordingOtpNotifier otpNotifier;
    @Autowired private PasswordEncoder passwordEncoder;

    private final Map<String, String> emailByToken = new ConcurrentHashMap<>();
    private String passwordHash;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("TRUNCATE otp_challenges, booking_seats, bookings, seats, events, users RESTART IDENTITY CASCADE");
        otpNotifier.reset();
        emailByToken.clear();
        if (passwordHash == null) {
            passwordHash = passwordEncoder.encode(PASSWORD);
        }
    }

    protected Long createUser(String email, Role role) {
        return jdbc.queryForObject(
                "INSERT INTO users (name, email, password_hash, role, email_verified) VALUES (?, ?, ?, ?, TRUE) RETURNING id",
                Long.class, "Test " + email, email, passwordHash, role.name());
    }

    /** Creates users in one batch; ids are returned in insertion order. */
    protected List<Long> createUsers(int count) {
        List<Object[]> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rows.add(new Object[]{"Racer " + i, "racer" + i + "@test.dev", passwordHash, Role.USER.name()});
        }
        jdbc.batchUpdate("INSERT INTO users (name, email, password_hash, role, email_verified) VALUES (?, ?, ?, ?, TRUE)", rows);
        return jdbc.queryForList("SELECT id FROM users WHERE email LIKE 'racer%' ORDER BY id", Long.class);
    }

    protected AdminEventResponse createEvent(int rows, int seatsPerRow) {
        return adminEventService.createEvent(new EventRequest(
                "Concurrency Night", "Integration test event", EventCategory.CONCERT, "Test Arena", "Chennai",
                LocalDate.now().plusDays(30), LocalTime.of(19, 0), null, true, rows, seatsPerRow,
                new SectionPricing(new BigDecimal("1500"), new BigDecimal("1000"), new BigDecimal("500"))));
    }

    protected List<Long> seatIds(Long eventId) {
        return jdbc.queryForList("SELECT id FROM seats WHERE event_id = ? ORDER BY row_label, seat_index", Long.class, eventId);
    }

    /** Full two-step sign-in: password, then the emailed code. Returns a Bearer header value. */
    protected String login(String email) throws Exception {
        JsonNode challenge = readJson(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("verification");

        String body = mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new VerificationCode(UUID.fromString(challenge.get("challengeId").asText()), otpNotifier.lastCodeFor(email)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + readJson(body).get("token").asText();
        emailByToken.put(token, email);
        return token;
    }

    /** Requests a code for a booking or cancellation and returns it as the user would type it. */
    protected VerificationCode requestCode(String token, Map<String, Object> request) throws Exception {
        String body = mockMvc.perform(post("/api/verifications")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID challengeId = UUID.fromString(readJson(body).get("challengeId").asText());
        return new VerificationCode(challengeId, otpNotifier.lastCodeFor(emailByToken.get(token)));
    }

    /** A booking request carrying a valid code for exactly these seats. */
    protected MockHttpServletRequestBuilder verifiedBooking(String token, Long eventId, List<Long> seatIds) throws Exception {
        VerificationCode code = requestCode(token, Map.of("purpose", "BOOKING", "eventId", eventId, "seatIds", seatIds));
        return post("/api/bookings")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("eventId", eventId, "seatIds", seatIds, "verification", code)));
    }

    /** A cancellation request carrying a valid code for this booking. */
    protected MockHttpServletRequestBuilder verifiedCancellation(String token, long bookingId) throws Exception {
        VerificationCode code = requestCode(token, Map.of("purpose", "CANCELLATION", "bookingId", bookingId));
        return post("/api/bookings/{id}/cancel", bookingId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("verification", code)));
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode readJson(String body) throws Exception {
        return objectMapper.readTree(body);
    }
}
