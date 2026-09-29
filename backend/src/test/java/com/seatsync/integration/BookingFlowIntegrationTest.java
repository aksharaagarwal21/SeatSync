package com.seatsync.integration;

import com.seatsync.dto.admin.AdminEventResponse;
import com.seatsync.entity.Role;
import com.seatsync.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller → Service → Repository → PostgreSQL, checking both HTTP responses and stored state. */
class BookingFlowIntegrationTest extends AbstractIntegrationTest {

    private AdminEventResponse event;
    private List<Long> seats;
    private String alice;
    private String bob;

    @BeforeEach
    void setUpEventAndUsers() throws Exception {
        event = createEvent(2, 10);
        seats = seatIds(event.id());
        createUser("alice@test.dev", Role.USER);
        createUser("bob@test.dev", Role.USER);
        alice = login("alice@test.dev");
        bob = login("bob@test.dev");
    }

    @Test
    void publicCanBrowseEventsAndSeatMap() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Concurrency Night"))
                .andExpect(jsonPath("$.content[0].totalSeats").value(20))
                .andExpect(jsonPath("$.content[0].minPrice").value(1000.0));

        mockMvc.perform(get("/api/events/{id}", event.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingOpen").value(true))
                .andExpect(jsonPath("$.sections.length()").value(2));

        mockMvc.perform(get("/api/events/{id}/seats", event.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20))
                .andExpect(jsonPath("$[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void bookingPersistsBookingAndMarksSeatsBooked() throws Exception {
        String body = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", event.id(), "seatIds", seats.subList(0, 2)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(3000.0))
                .andExpect(jsonPath("$.seats[0].seatNumber").value("A1"))
                .andReturn().getResponse().getContentAsString();
        long bookingId = readJson(body).get("id").asLong();

        assertThat(jdbc.queryForList("SELECT status FROM seats WHERE id IN (?, ?)", String.class, seats.get(0), seats.get(1)))
                .containsOnly("BOOKED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM booking_seats WHERE booking_id = ? AND active", Long.class, bookingId))
                .isEqualTo(2L);

        mockMvc.perform(get("/api/bookings/me").header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(bookingId));
    }

    @Test
    void secondUserGetsConflictForAlreadyBookedSeat() throws Exception {
        book(alice, seats.get(0));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", event.id(), "seatIds", List.of(seats.get(0), seats.get(1))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Booking Conflict"))
                .andExpect(jsonPath("$.message").value("Seat A1 is no longer available."));

        assertThat(jdbc.queryForObject("SELECT status FROM seats WHERE id = ?", String.class, seats.get(1)))
                .as("the free seat in the rejected request must not be partially booked")
                .isEqualTo("AVAILABLE");
    }

    @Test
    void heldSeatsAreVisibleAsReservedAndBlockOthers() throws Exception {
        mockMvc.perform(post("/api/holds")
                        .header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", event.id(), "seatIds", List.of(seats.get(3))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[0].heldByMe").value(true))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());

        mockMvc.perform(get("/api/events/{id}/seats", event.id()).header("Authorization", bob))
                .andExpect(jsonPath("$[3].status").value("RESERVED"))
                .andExpect(jsonPath("$[3].heldByMe").value(false));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", event.id(), "seatIds", List.of(seats.get(3))))))
                .andExpect(status().isConflict());

        book(alice, seats.get(3));
    }

    @Test
    void cancellationFreesSeatsForOtherUsers() throws Exception {
        long bookingId = book(alice, seats.get(5));

        mockMvc.perform(delete("/api/bookings/{id}", bookingId).header("Authorization", bob))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/bookings/{id}", bookingId).header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(jdbc.queryForObject("SELECT status FROM bookings WHERE id = ?", String.class, bookingId)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT status FROM seats WHERE id = ?", String.class, seats.get(5))).isEqualTo("AVAILABLE");

        book(bob, seats.get(5));
    }

    @Test
    void invalidBookingRequestIsRejectedBeforeBusinessLogic() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", event.id(), "seatIds", List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.seatIds").value("Select at least one seat"));
    }

    @Test
    void seatsFromAnotherEventAreNotFound() throws Exception {
        AdminEventResponse other = createEvent(1, 5);
        Long foreignSeat = seatIds(other.id()).getFirst();

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", event.id(), "seatIds", List.of(foreignSeat)))))
                .andExpect(status().isNotFound());
    }

    private long book(String token, Long seatId) throws Exception {
        String body = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", event.id(), "seatIds", List.of(seatId)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readJson(body).get("id").asLong();
    }
}
