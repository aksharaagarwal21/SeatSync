package com.seatsync.controller;

import com.seatsync.dto.seat.HoldResponse;
import com.seatsync.dto.seat.SeatSelectionRequest;
import com.seatsync.security.AuthenticatedUser;
import com.seatsync.service.SeatHoldService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holds")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    public SeatHoldController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @PostMapping
    public HoldResponse holdSeats(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody SeatSelectionRequest request) {
        return seatHoldService.holdSeats(user.id(), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> releaseHolds(@AuthenticationPrincipal AuthenticatedUser user,
                                             @RequestParam Long eventId) {
        seatHoldService.releaseHolds(user.id(), eventId);
        return ResponseEntity.noContent().build();
    }
}
