package com.seatsync.support;

import com.seatsync.entity.Event;
import com.seatsync.entity.EventCategory;
import com.seatsync.entity.EventDetails;
import com.seatsync.entity.EventStatus;
import com.seatsync.entity.Role;
import com.seatsync.entity.Seat;
import com.seatsync.entity.SeatSection;
import com.seatsync.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** In-memory entities for unit tests (no database, ids set directly). */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user(long id, Role role) {
        User user = new User("User " + id, "user" + id + "@test.dev", "hash", role, Instant.EPOCH);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Event event(long id, LocalDate date, EventStatus status) {
        EventDetails details = new EventDetails("Test Event " + id, "Description", EventCategory.CONCERT,
                "Test Venue", "Chennai", date, LocalTime.of(19, 0), null);
        Event event = new Event(details, status, 2, 10, Instant.EPOCH);
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    public static Seat seat(long id, Event event, String row, int number, String price) {
        Seat seat = new Seat(event, row, number, SeatSection.STANDARD, new BigDecimal(price));
        ReflectionTestUtils.setField(seat, "id", id);
        return seat;
    }
}
