package com.seatsync.service.layout;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inserts generated seats with JDBC batching. Seat ids are IDENTITY columns, which disables
 * Hibernate insert batching, so layouts of hundreds of seats go through JdbcTemplate instead.
 * Runs on the caller's transactional connection.
 */
@Component
public class SeatBatchWriter {

    private static final String INSERT_SQL = """
            INSERT INTO seats (event_id, seat_number, row_label, seat_index, section, price, status, version)
            VALUES (?, ?, ?, ?, ?, ?, 'AVAILABLE', 0)
            """;
    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;

    public SeatBatchWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Long eventId, List<SeatLayoutPlanner.PlannedSeat> seats) {
        jdbcTemplate.batchUpdate(INSERT_SQL, seats, BATCH_SIZE, (statement, seat) -> {
            statement.setLong(1, eventId);
            statement.setString(2, seat.seatNumber());
            statement.setString(3, seat.row());
            statement.setInt(4, seat.number());
            statement.setString(5, seat.section().name());
            statement.setBigDecimal(6, seat.price());
        });
    }
}
