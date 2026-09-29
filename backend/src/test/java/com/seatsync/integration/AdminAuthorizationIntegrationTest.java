package com.seatsync.integration;

import com.seatsync.entity.Role;
import com.seatsync.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuthorizationIntegrationTest extends AbstractIntegrationTest {

    private String admin;
    private String user;

    @BeforeEach
    void setUpAccounts() throws Exception {
        createUser("admin@test.dev", Role.ADMIN);
        createUser("user@test.dev", Role.USER);
        admin = login("admin@test.dev");
        user = login("user@test.dev");
    }

    @Test
    void regularUsersCannotReachAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", user)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/bookings").header("Authorization", user)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/events").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON).content(json(eventBody(5, 10, true))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreatesEventWithGeneratedSeatLayout() throws Exception {
        String body = mockMvc.perform(post("/api/admin/events").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(eventBody(5, 10, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalSeats").value(50))
                .andExpect(jsonPath("$.pricing.vip").value(2000.0))
                .andReturn().getResponse().getContentAsString();
        long eventId = readJson(body).get("id").asLong();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM seats WHERE event_id = ?", Long.class, eventId)).isEqualTo(50L);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT seat_number) FROM seats WHERE event_id = ?", Long.class, eventId)).isEqualTo(50L);
    }

    @Test
    void adminCanPauseSalesAndBookingIsThenRejected() throws Exception {
        long eventId = createEvent(1, 5).id();

        mockMvc.perform(put("/api/admin/events/{id}", eventId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(eventBody(1, 5, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(post("/api/bookings").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", eventId, "seatIds", seatIds(eventId).subList(0, 1)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Booking is closed for this event."));
    }

    @Test
    void eventWithBookingsCannotBeDeleted() throws Exception {
        long eventId = createEvent(1, 5).id();
        mockMvc.perform(post("/api/bookings").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", eventId, "seatIds", seatIds(eventId).subList(0, 1)))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/admin/events/{id}", eventId).header("Authorization", admin))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedBookings").value(1))
                .andExpect(jsonPath("$.ticketsSold").value(1))
                .andExpect(jsonPath("$.dailyBookings.length()").value(14));
    }

    private Map<String, Object> eventBody(int rows, int seatsPerRow, boolean bookingOpen) {
        return Map.ofEntries(
                Map.entry("name", "Admin Created Show"),
                Map.entry("description", "Created by an admin"),
                Map.entry("category", "THEATRE"),
                Map.entry("venue", "Test Hall"),
                Map.entry("city", "Pune"),
                Map.entry("eventDate", java.time.LocalDate.now().plusDays(20).toString()),
                Map.entry("startTime", "19:30"),
                Map.entry("imageUrl", ""),
                Map.entry("bookingOpen", bookingOpen),
                Map.entry("rows", rows),
                Map.entry("seatsPerRow", seatsPerRow),
                Map.entry("pricing", Map.of("vip", 2000, "premium", 1200, "standard", 600)));
    }
}
