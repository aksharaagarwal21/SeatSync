package com.seatsync.service;

import com.seatsync.dto.PageResponse;
import com.seatsync.dto.admin.AdminEventResponse;
import com.seatsync.dto.admin.EventRequest;
import com.seatsync.entity.Event;
import com.seatsync.entity.EventStatus;
import com.seatsync.entity.SeatSection;
import com.seatsync.entity.SeatStatus;
import com.seatsync.exception.BusinessRuleException;
import com.seatsync.mapper.EventMapper;
import com.seatsync.repository.BookingRepository;
import com.seatsync.repository.EventRepository;
import com.seatsync.repository.EventSpecifications;
import com.seatsync.repository.SeatRepository;
import com.seatsync.repository.projection.EventSeatStats;
import com.seatsync.service.layout.SeatBatchWriter;
import com.seatsync.service.layout.SeatLayoutPlanner;
import com.seatsync.service.realtime.SeatsChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AdminEventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final EventService eventService;
    private final EventMapper eventMapper;
    private final SeatLayoutPlanner layoutPlanner;
    private final SeatBatchWriter seatBatchWriter;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AdminEventService(EventRepository eventRepository,
                             SeatRepository seatRepository,
                             BookingRepository bookingRepository,
                             EventService eventService,
                             EventMapper eventMapper,
                             SeatLayoutPlanner layoutPlanner,
                             SeatBatchWriter seatBatchWriter,
                             ApplicationEventPublisher eventPublisher,
                             Clock clock) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.eventService = eventService;
        this.eventMapper = eventMapper;
        this.layoutPlanner = layoutPlanner;
        this.seatBatchWriter = seatBatchWriter;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminEventResponse> listEvents(String query, boolean includePast, int page, int size) {
        LocalDate from = includePast ? null : LocalDate.now(clock);
        Page<Event> events = eventRepository.findAll(
                EventSpecifications.matching(query, null, null, from, null),
                PageRequest.of(page, size, Sort.by("eventDate", "startTime", "id")));
        Map<Long, EventSeatStats> stats = eventService.loadStats(events.getContent().stream().map(Event::getId).toList());
        List<AdminEventResponse> content = events.getContent().stream()
                .map(event -> eventMapper.toAdminResponse(event, EventService.statsFor(stats, event.getId()), List.of()))
                .toList();
        return PageResponse.of(events, content);
    }

    @Transactional(readOnly = true)
    public AdminEventResponse getEvent(Long eventId) {
        return toResponse(eventService.findEvent(eventId));
    }

    @Transactional
    public AdminEventResponse createEvent(EventRequest request) {
        if (request.eventDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessRuleException("Event date can't be in the past.");
        }
        Event event = eventRepository.save(new Event(
                eventMapper.toDetails(request), statusOf(request), request.rows(), request.seatsPerRow(), clock.instant()));
        seatBatchWriter.insert(event.getId(), layoutPlanner.plan(request.rows(), request.seatsPerRow(), request.pricing()));
        return toResponse(event);
    }

    /**
     * Details and prices can always change. The seat layout can only change while nothing has
     * ever been booked or held, because existing bookings reference individual seats.
     */
    @Transactional
    public AdminEventResponse updateEvent(Long eventId, EventRequest request) {
        Event event = eventService.findEvent(eventId);
        event.update(eventMapper.toDetails(request), statusOf(request));

        if (event.hasLayout(request.rows(), request.seatsPerRow())) {
            for (SeatSection section : SeatSection.values()) {
                seatRepository.updateSectionPrice(eventId, section, request.pricing().priceFor(section));
            }
        } else {
            if (bookingRepository.existsByEventId(eventId) || seatRepository.existsByEventIdAndStatusNot(eventId, SeatStatus.AVAILABLE)) {
                throw new BusinessRuleException("The seat layout can't be changed after seats have been booked or held.");
            }
            event.changeLayout(request.rows(), request.seatsPerRow());
            eventRepository.flush();
            seatRepository.deleteByEventId(eventId);
            seatBatchWriter.insert(eventId, layoutPlanner.plan(request.rows(), request.seatsPerRow(), request.pricing()));
        }

        eventRepository.flush();
        eventPublisher.publishEvent(SeatsChangedEvent.of(eventId, List.of()));
        return toResponse(eventService.findEvent(eventId));
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventService.findEvent(eventId);
        if (bookingRepository.existsByEventId(eventId)) {
            throw new BusinessRuleException("This event has bookings and can't be deleted. Pause sales instead.");
        }
        eventRepository.delete(event);
    }

    private AdminEventResponse toResponse(Event event) {
        EventSeatStats stats = EventService.statsFor(eventService.loadStats(List.of(event.getId())), event.getId());
        return eventMapper.toAdminResponse(event, stats, seatRepository.findSectionStats(event.getId()));
    }

    private static EventStatus statusOf(EventRequest request) {
        return request.bookingOpen() ? EventStatus.ON_SALE : EventStatus.PAUSED;
    }
}
