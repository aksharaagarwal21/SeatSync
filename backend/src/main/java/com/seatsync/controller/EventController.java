package com.seatsync.controller;

import com.seatsync.dto.PageResponse;
import com.seatsync.dto.event.EventDetailResponse;
import com.seatsync.dto.event.EventFiltersResponse;
import com.seatsync.dto.event.EventSummaryResponse;
import com.seatsync.dto.seat.SeatResponse;
import com.seatsync.entity.EventCategory;
import com.seatsync.security.AuthenticatedUser;
import com.seatsync.service.EventService;
import com.seatsync.service.realtime.SeatUpdateBroadcaster;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@Validated
public class EventController {

    private final EventService eventService;
    private final SeatUpdateBroadcaster seatUpdateBroadcaster;

    public EventController(EventService eventService, SeatUpdateBroadcaster seatUpdateBroadcaster) {
        this.eventService = eventService;
        this.seatUpdateBroadcaster = seatUpdateBroadcaster;
    }

    @GetMapping
    public PageResponse<EventSummaryResponse> searchEvents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(48) int size) {
        return eventService.searchEvents(q, category, city, from, to, page, size);
    }

    @GetMapping("/filters")
    public EventFiltersResponse getFilters() {
        return eventService.getFilters();
    }

    @GetMapping("/{eventId}")
    public EventDetailResponse getEvent(@PathVariable Long eventId) {
        return eventService.getEvent(eventId);
    }

    @GetMapping("/{eventId}/seats")
    public List<SeatResponse> getSeats(@PathVariable Long eventId, @AuthenticationPrincipal AuthenticatedUser viewer) {
        return eventService.getSeatMap(eventId, viewer == null ? null : viewer.id());
    }

    @GetMapping(path = "/{eventId}/seats/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSeatUpdates(@PathVariable Long eventId) {
        eventService.ensureExists(eventId);
        return seatUpdateBroadcaster.subscribe(eventId);
    }
}
