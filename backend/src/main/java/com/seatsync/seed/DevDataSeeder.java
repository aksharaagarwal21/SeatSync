package com.seatsync.seed;

import com.seatsync.dto.admin.SectionPricing;
import com.seatsync.service.layout.SeatLayoutPlanner;
import com.seatsync.service.layout.SeatLayoutPlanner.PlannedSeat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Seeds a realistic dataset for local development and load testing: 50 events × 200 seats
 * (10,000 seats), demo accounts, 500 load-test accounts and a history of bookings.
 * Runs only with the {@code dev} profile and only against an empty database.
 * A fixed random seed keeps the data identical between runs.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "seatsync.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    public static final String ADMIN_EMAIL = "admin@seatsync.dev";
    public static final String DEMO_EMAIL = "demo@seatsync.dev";
    public static final String ADMIN_PASSWORD = "Admin@12345";
    public static final String DEMO_PASSWORD = "Demo@12345";
    public static final String LOAD_TEST_PASSWORD = "LoadTest@123";
    public static final int LOAD_TEST_USERS = 500;

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final int EVENT_COUNT = 50;
    private static final int ROWS = 10;
    private static final int SEATS_PER_ROW = 20;
    private static final int PAST_EVENTS = 4;
    private static final String REFERENCE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final SeatLayoutPlanner layoutPlanner;
    private final Clock clock;
    private final Random random = new Random(42);

    public DevDataSeeder(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, SeatLayoutPlanner layoutPlanner, Clock clock) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.layoutPlanner = layoutPlanner;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long existing = jdbc.queryForObject("SELECT count(*) FROM events", Long.class);
        if (existing != null && existing > 0) {
            log.info("Seed skipped: database already has {} events", existing);
            return;
        }
        long started = System.currentTimeMillis();
        List<Long> customerIds = seedUsers();
        List<SeededEvent> events = seedEvents();
        int bookings = seedBookings(events, customerIds);
        log.info("Seeded {} events, {} seats, {} bookings in {} ms",
                events.size(), events.size() * ROWS * SEATS_PER_ROW, bookings, System.currentTimeMillis() - started);
    }

    private List<Long> seedUsers() {
        Instant now = clock.instant();
        insertUser("SeatSync Admin", ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), "ADMIN", now.minus(Duration.ofDays(60)));
        List<Long> customers = new ArrayList<>();
        customers.add(insertUser("Demo User", DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "USER", now.minus(Duration.ofDays(45))));

        String sharedHash = passwordEncoder.encode(DEMO_PASSWORD);
        for (String name : SeedCatalog.CUSTOMER_NAMES) {
            String email = name.toLowerCase().replace(' ', '.') + "@example.com";
            customers.add(insertUser(name, email, sharedHash, "USER", now.minus(Duration.ofDays(5 + random.nextInt(40)))));
        }

        // One hash for all load-test accounts: BCrypt is deliberately slow, 500 hashes would take ~40s.
        String loadTestHash = passwordEncoder.encode(LOAD_TEST_PASSWORD);
        List<Object[]> loadTestUsers = new ArrayList<>(LOAD_TEST_USERS);
        for (int i = 1; i <= LOAD_TEST_USERS; i++) {
            loadTestUsers.add(new Object[]{"Load Tester " + i, "loadtest" + i + "@seatsync.dev", loadTestHash, "USER", Timestamp.from(now)});
        }
        jdbc.batchUpdate("INSERT INTO users (name, email, password_hash, role, created_at, email_verified) VALUES (?, ?, ?, ?, ?, TRUE)", loadTestUsers);
        return customers;
    }

    private Long insertUser(String name, String email, String hash, String role, Instant createdAt) {
        return jdbc.queryForObject(
                "INSERT INTO users (name, email, password_hash, role, created_at, email_verified) VALUES (?, ?, ?, ?, ?, TRUE) RETURNING id",
                Long.class, name, email, hash, role, Timestamp.from(createdAt));
    }

    private List<SeededEvent> seedEvents() {
        LocalDate today = LocalDate.now(clock);
        List<SeedCatalog.EventTemplate> templates = new ArrayList<>(SeedCatalog.EVENTS);
        Collections.shuffle(templates, random);
        List<SeededEvent> events = new ArrayList<>(EVENT_COUNT);

        for (int i = 0; i < EVENT_COUNT; i++) {
            SeedCatalog.EventTemplate template = templates.get(i % templates.size());
            SeedCatalog.Venue venue = SeedCatalog.VENUES.get(random.nextInt(SeedCatalog.VENUES.size()));
            boolean past = i < PAST_EVENTS;
            LocalDate date = past ? today.minusDays(3 + random.nextInt(20)) : today.plusDays(2 + (i * 88L / EVENT_COUNT) + random.nextInt(3));
            LocalTime time = SeedCatalog.startTimeFor(template.category(), random);
            String status = !past && i % 17 == 9 ? "PAUSED" : "ON_SALE";
            String image = SeedCatalog.imageFor(template.category(), i);

            Long eventId = jdbc.queryForObject("""
                            INSERT INTO events (name, description, category, venue, city, event_date, start_time,
                                                image_url, status, row_count, seats_per_row, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                            """, Long.class,
                    template.name(), template.description(), template.category().name(), venue.name(), venue.city(),
                    Date.valueOf(date), Time.valueOf(time), image, status, ROWS, SEATS_PER_ROW,
                    Timestamp.from(clock.instant().minus(Duration.ofDays(30))));

            SectionPricing pricing = SeedCatalog.pricingFor(template.category());
            List<PlannedSeat> plan = layoutPlanner.plan(ROWS, SEATS_PER_ROW, pricing);
            List<Object[]> seatRows = plan.stream()
                    .map(seat -> new Object[]{eventId, seat.seatNumber(), seat.row(), seat.number(), seat.section().name(), seat.price()})
                    .toList();
            jdbc.batchUpdate("""
                    INSERT INTO seats (event_id, seat_number, row_label, seat_index, section, price, status, version)
                    VALUES (?, ?, ?, ?, ?, ?, 'AVAILABLE', 0)
                    """, seatRows);

            List<Long> seatIds = jdbc.queryForList("SELECT id FROM seats WHERE event_id = ? ORDER BY row_label, seat_index", Long.class, eventId);
            List<BigDecimal> prices = plan.stream().map(PlannedSeat::price).toList();
            events.add(new SeededEvent(eventId, date, time, seatIds, prices, occupancyFor(i)));
        }
        return events;
    }

    /** A spread of demand: one sold-out show, a few nearly full, most partially sold. */
    private double occupancyFor(int index) {
        if (index == PAST_EVENTS + 3) {
            return 1.0;
        }
        if (index % 11 == 5) {
            return 0.88 + random.nextDouble() * 0.08;
        }
        return 0.05 + random.nextDouble() * 0.55;
    }

    private int seedBookings(List<SeededEvent> events, List<Long> customerIds) {
        Instant now = clock.instant();
        Long demoUserId = customerIds.getFirst();
        int bookingCount = 0;

        for (SeededEvent event : events) {
            int target = (int) Math.round(event.seatIds().size() * event.occupancy());
            int booked = 0;
            Instant eventStart = event.date().atTime(event.time()).atZone(clock.getZone()).toInstant();
            Instant latest = eventStart.isBefore(now) ? eventStart.minus(Duration.ofDays(1)) : now;

            for (List<Integer> seatIndexes : seatGroupsInSaleOrder()) {
                if (booked >= target) {
                    break;
                }
                booked += seatIndexes.size();

                boolean demoBooking = bookingCount % 37 == 0;
                Long userId = demoBooking ? demoUserId : customerIds.get(random.nextInt(customerIds.size()));
                boolean cancelled = random.nextInt(100) < 6 && event.occupancy() < 1.0;
                Instant bookedAt = latest.minus(Duration.ofMinutes(random.nextInt(60 * 24 * 20)));
                insertBooking(userId, event, seatIndexes, bookedAt, cancelled);
                bookingCount++;
            }
        }
        return bookingCount;
    }

    /**
     * Splits each row into groups of 1–4 adjacent seats and orders the groups roughly front to
     * back with some noise, so front rows sell first but the map doesn't fill in a strict line.
     */
    private List<List<Integer>> seatGroupsInSaleOrder() {
        record Group(List<Integer> seatIndexes, double saleOrder) {
        }
        List<Group> groups = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            int seat = 0;
            while (seat < SEATS_PER_ROW) {
                int size = Math.min(1 + random.nextInt(4), SEATS_PER_ROW - seat);
                List<Integer> indexes = new ArrayList<>(size);
                for (int k = 0; k < size; k++) {
                    indexes.add(row * SEATS_PER_ROW + seat + k);
                }
                groups.add(new Group(indexes, row + random.nextDouble() * 4));
                seat += size;
            }
        }
        return groups.stream()
                .sorted(Comparator.comparingDouble(Group::saleOrder))
                .map(Group::seatIndexes)
                .toList();
    }

    private void insertBooking(Long userId, SeededEvent event, List<Integer> seatIndexes, Instant bookedAt, boolean cancelled) {
        BigDecimal total = seatIndexes.stream().map(event.prices()::get).reduce(BigDecimal.ZERO, BigDecimal::add);
        Long bookingId = jdbc.queryForObject("""
                        INSERT INTO bookings (reference, user_id, event_id, total_amount, status, booking_time, cancelled_at, version)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 0) RETURNING id
                        """, Long.class,
                nextReference(), userId, event.id(), total, cancelled ? "CANCELLED" : "CONFIRMED",
                Timestamp.from(bookedAt), cancelled ? Timestamp.from(bookedAt.plus(Duration.ofHours(6))) : null);

        List<Object[]> lines = seatIndexes.stream()
                .map(index -> new Object[]{bookingId, event.seatIds().get(index), event.prices().get(index), !cancelled})
                .toList();
        jdbc.batchUpdate("INSERT INTO booking_seats (booking_id, seat_id, price, active) VALUES (?, ?, ?, ?)", lines);
        if (!cancelled) {
            List<Object[]> seatIds = seatIndexes.stream().map(index -> new Object[]{event.seatIds().get(index)}).toList();
            jdbc.batchUpdate("UPDATE seats SET status = 'BOOKED', version = version + 1 WHERE id = ?", seatIds);
        }
    }

    private String nextReference() {
        StringBuilder reference = new StringBuilder("SS-");
        for (int i = 0; i < 8; i++) {
            reference.append(REFERENCE_ALPHABET.charAt(random.nextInt(REFERENCE_ALPHABET.length())));
        }
        return reference.toString();
    }

    private record SeededEvent(Long id, LocalDate date, LocalTime time,
                               List<Long> seatIds, List<BigDecimal> prices, double occupancy) {
    }
}
