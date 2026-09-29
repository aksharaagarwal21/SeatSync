package com.seatsync.service;

import com.seatsync.dto.PageResponse;
import com.seatsync.dto.event.EventDetailResponse;
import com.seatsync.dto.event.EventFiltersResponse;
import com.seatsync.dto.event.EventSummaryResponse;
import com.seatsync.dto.seat.SeatResponse;
import com.seatsync.entity.Event;
import com.seatsync.entity.EventCategory;
import com.seatsync.exception.ResourceNotFoundException;
import com.seatsync.mapper.EventMapper;
import com.seatsync.mapper.SeatMapper;
import com.seatsync.repository.EventRepository;
import com.seatsync.repository.EventSpecifications;
import com.seatsync.repository.SeatRepository;
import com.seatsync.repository.projection.EventSeatStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EventService {

    private static final Sort EVENT_ORDER = Sort.by("eventDate", "startTime", "id");

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventMapper eventMapper;
    private final SeatMapper seatMapper;
    private final BookingPolicy bookingPolicy;
    private final Clock clock;

    public EventService(EventRepository eventRepository,
                        SeatRepository seatRepository,
                        EventMapper eventMapper,
                        SeatMapper seatMapper,
                        BookingPolicy bookingPolicy,
                        Clock clock) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.eventMapper = eventMapper;
        this.seatMapper = seatMapper;
        this.bookingPolicy = bookingPolicy;
        this.clock = clock;
    }

    /** Upcoming events only: {@code from} is clamped to today. */
    public PageResponse<EventSummaryResponse> searchEvents(String query, EventCategory category, String city,
                                                           LocalDate from, LocalDate to, int page, int size) {
        LocalDate today = LocalDate.now(clock);
        LocalDate effectiveFrom = from == null || from.isBefore(today) ? today : from;
        Page<Event> events = eventRepository.findAll(
                EventSpecifications.matching(query, category, city, effectiveFrom, to),
                PageRequest.of(page, size, EVENT_ORDER));
        Map<Long, EventSeatStats> stats = loadStats(events.getContent().stream().map(Event::getId).toList());
        List<EventSummaryResponse> content = events.getContent().stream()
                .map(event -> eventMapper.toSummary(event, statsFor(stats, event.getId()), bookingPolicy.isBookable(event)))
                .toList();
        return PageResponse.of(events, content);
    }

    public EventDetailResponse getEvent(Long eventId) {
        Event event = findEvent(eventId);
        EventSeatStats stats = statsFor(loadStats(List.of(eventId)), eventId);
        return eventMapper.toDetail(event, stats, seatRepository.findSectionStats(eventId), bookingPolicy.isBookable(event));
    }

    public List<SeatResponse> getSeatMap(Long eventId, Long viewerId) {
        ensureExists(eventId);
        Instant now = bookingPolicy.now();
        return seatRepository.findSeatMap(eventId).stream()
                .map(seat -> seatMapper.toResponse(seat, viewerId, now))
                .toList();
    }

    public EventFiltersResponse getFilters() {
        return new EventFiltersResponse(
                Arrays.asList(EventCategory.values()),
                eventRepository.findCitiesWithEventsFrom(LocalDate.now(clock)));
    }

    public void ensureExists(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw ResourceNotFoundException.event(eventId);
        }
    }

    Event findEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> ResourceNotFoundException.event(eventId));
    }

    Map<Long, EventSeatStats> loadStats(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return seatRepository.findStatsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(EventSeatStats::eventId, Function.identity()));
    }

    static EventSeatStats statsFor(Map<Long, EventSeatStats> stats, Long eventId) {
        return stats.getOrDefault(eventId, EventSeatStats.empty(eventId));
    }
}
