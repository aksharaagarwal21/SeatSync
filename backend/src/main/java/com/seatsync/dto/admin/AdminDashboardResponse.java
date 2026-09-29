package com.seatsync.dto.admin;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(
        long totalEvents,
        long upcomingEvents,
        long confirmedBookings,
        long ticketsSold,
        long availableSeats,
        List<DailyBookings> dailyBookings,
        List<AdminBookingResponse> recentBookings
) {

    public record DailyBookings(LocalDate date, long bookings, long tickets) {
    }
}
