package com.seatsync.service;

import com.seatsync.dto.admin.AdminDashboardResponse;
import com.seatsync.dto.admin.AdminDashboardResponse.DailyBookings;
import com.seatsync.entity.BookingStatus;
import com.seatsync.repository.BookingRepository;
import com.seatsync.repository.BookingSeatRepository;
import com.seatsync.repository.EventRepository;
import com.seatsync.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int CHART_DAYS = 14;
    private static final int RECENT_BOOKINGS = 6;

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingService bookingService;
    private final Clock clock;

    public AdminDashboardService(EventRepository eventRepository,
                                 SeatRepository seatRepository,
                                 BookingRepository bookingRepository,
                                 BookingSeatRepository bookingSeatRepository,
                                 BookingService bookingService,
                                 Clock clock) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingService = bookingService;
        this.clock = clock;
    }

    public AdminDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(clock);
        return new AdminDashboardResponse(
                eventRepository.count(),
                eventRepository.countByEventDateGreaterThanEqual(today),
                bookingRepository.countByStatus(BookingStatus.CONFIRMED),
                bookingSeatRepository.countByActiveTrue(),
                seatRepository.countAvailableFrom(today),
                dailyBookings(today),
                bookingService.searchBookings(null, null, 0, RECENT_BOOKINGS).content());
    }

    /** One entry per day for the last two weeks, including days without bookings. */
    private List<DailyBookings> dailyBookings(LocalDate today) {
        LocalDate firstDay = today.minusDays(CHART_DAYS - 1L);
        Map<LocalDate, DailyBookings> byDay = new HashMap<>();
        for (Object[] row : bookingRepository.countDailyConfirmed(firstDay.atStartOfDay(clock.getZone()).toInstant(), clock.getZone().getId())) {
            LocalDate day = LocalDate.parse((String) row[0]);
            byDay.put(day, new DailyBookings(day, ((Number) row[1]).longValue(), ((Number) row[2]).longValue()));
        }
        List<DailyBookings> series = new ArrayList<>(CHART_DAYS);
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            series.add(byDay.getOrDefault(day, new DailyBookings(day, 0, 0)));
        }
        return series;
    }
}
