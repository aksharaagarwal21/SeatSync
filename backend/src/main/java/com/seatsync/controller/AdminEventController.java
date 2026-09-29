package com.seatsync.controller;

import com.seatsync.dto.PageResponse;
import com.seatsync.dto.admin.AdminEventResponse;
import com.seatsync.dto.admin.EventRequest;
import com.seatsync.service.AdminEventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/events")
@Validated
public class AdminEventController {

    private final AdminEventService adminEventService;

    public AdminEventController(AdminEventService adminEventService) {
        this.adminEventService = adminEventService;
    }

    @GetMapping
    public PageResponse<AdminEventResponse> listEvents(@RequestParam(required = false) String q,
                                                       @RequestParam(defaultValue = "false") boolean includePast,
                                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminEventService.listEvents(q, includePast, page, size);
    }

    @GetMapping("/{eventId}")
    public AdminEventResponse getEvent(@PathVariable Long eventId) {
        return adminEventService.getEvent(eventId);
    }

    @PostMapping
    public ResponseEntity<AdminEventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminEventService.createEvent(request));
    }

    @PutMapping("/{eventId}")
    public AdminEventResponse updateEvent(@PathVariable Long eventId, @Valid @RequestBody EventRequest request) {
        return adminEventService.updateEvent(eventId, request);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        adminEventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
