package com.seatsync.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatsync.dto.admin.AdminEventResponse;
import com.seatsync.dto.admin.EventRequest;
import com.seatsync.dto.admin.SectionPricing;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full application context against PostgreSQL in Docker. Every test starts from empty tables. */
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
    @Autowired private PasswordEncoder passwordEncoder;

    private String passwordHash;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("TRUNCATE booking_seats, bookings, seats, events, users RESTART IDENTITY CASCADE");
        if (passwordHash == null) {
            passwordHash = passwordEncoder.encode(PASSWORD);
        }
    }

    protected Long createUser(String email, Role role) {
        return jdbc.queryForObject(
                "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, "Test " + email, email, passwordHash, role.name());
    }

    /** Creates users in one batch; ids are returned in insertion order. */
    protected List<Long> createUsers(int count) {
        List<Object[]> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rows.add(new Object[]{"Racer " + i, "racer" + i + "@test.dev", passwordHash, Role.USER.name()});
        }
        jdbc.batchUpdate("INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)", rows);
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

    protected String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginBody(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("token").asText();
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode readJson(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private record LoginBody(String email, String password) {
    }
}
