package com.seatsync.controller;

import com.seatsync.dto.PageResponse;
import com.seatsync.dto.admin.AdminBookingResponse;
import com.seatsync.dto.admin.AdminDashboardResponse;
import com.seatsync.dto.admin.AdminUserResponse;
import com.seatsync.entity.BookingStatus;
import com.seatsync.service.AdminDashboardService;
import com.seatsync.service.BookingService;
import com.seatsync.service.UserService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final BookingService bookingService;
    private final UserService userService;

    public AdminController(AdminDashboardService dashboardService, BookingService bookingService, UserService userService) {
        this.dashboardService = dashboardService;
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/bookings")
    public PageResponse<AdminBookingResponse> getBookings(@RequestParam(required = false) Long eventId,
                                                          @RequestParam(required = false) BookingStatus status,
                                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return bookingService.searchBookings(eventId, status, page, size);
    }

    @GetMapping("/users")
    public PageResponse<AdminUserResponse> getUsers(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return userService.listUsers(page, size);
    }
}
