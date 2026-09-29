package com.seatsync.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HoldExpiryJob {

    private final SeatHoldService seatHoldService;

    public HoldExpiryJob(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @Scheduled(fixedDelayString = "${seatsync.booking.hold-sweep-interval}")
    public void releaseExpiredHolds() {
        seatHoldService.releaseExpiredHolds();
    }
}
