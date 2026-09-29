package com.seatsync.mapper;

import com.seatsync.dto.admin.AdminEventResponse;
import com.seatsync.dto.admin.SectionPricing;
import com.seatsync.dto.event.EventDetailResponse;
import com.seatsync.dto.event.EventSummaryResponse;
import com.seatsync.dto.event.SectionAvailabilityResponse;
import com.seatsync.entity.Event;
import com.seatsync.entity.EventDetails;
import com.seatsync.entity.SeatSection;
import com.seatsync.dto.admin.EventRequest;
import com.seatsync.repository.projection.EventSeatStats;
import com.seatsync.repository.projection.SectionStats;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class EventMapper {

    public EventSummaryResponse toSummary(Event event, EventSeatStats stats, boolean bookingOpen) {
        return new EventSummaryResponse(
                event.getId(), event.getName(), event.getCategory(), event.getVenue(), event.getCity(),
                event.getEventDate(), event.getStartTime(), event.getImageUrl(), event.getStatus(), bookingOpen,
                stats.minPrice(), stats.maxPrice(), stats.totalSeats(), stats.availableSeats());
    }

    public EventDetailResponse toDetail(Event event, EventSeatStats stats, List<SectionStats> sections, boolean bookingOpen) {
        List<SectionAvailabilityResponse> sectionResponses = sections.stream()
                .sorted(Comparator.comparing(SectionStats::section))
                .map(section -> new SectionAvailabilityResponse(
                        section.section(), section.price(), section.totalSeats(), section.availableSeats()))
                .toList();
        return new EventDetailResponse(
                event.getId(), event.getName(), event.getDescription(), event.getCategory(), event.getVenue(),
                event.getCity(), event.getEventDate(), event.getStartTime(), event.getImageUrl(), event.getStatus(),
                bookingOpen, stats.minPrice(), stats.maxPrice(), stats.totalSeats(), stats.availableSeats(),
                sectionResponses);
    }

    public AdminEventResponse toAdminResponse(Event event, EventSeatStats stats, List<SectionStats> sections) {
        return new AdminEventResponse(
                event.getId(), event.getName(), event.getDescription(), event.getCategory(), event.getVenue(),
                event.getCity(), event.getEventDate(), event.getStartTime(), event.getImageUrl(), event.getStatus(),
                event.getRowCount(), event.getSeatsPerRow(), toPricing(sections),
                stats.totalSeats(), stats.availableSeats(), stats.heldSeats(), stats.bookedSeats());
    }

    public EventDetails toDetails(EventRequest request) {
        return new EventDetails(
                request.name().trim(),
                request.description().trim(),
                request.category(),
                request.venue().trim(),
                request.city().trim(),
                request.eventDate(),
                request.startTime(),
                request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().trim());
    }

    private static SectionPricing toPricing(List<SectionStats> sections) {
        return new SectionPricing(
                priceOf(sections, SeatSection.VIP),
                priceOf(sections, SeatSection.PREMIUM),
                priceOf(sections, SeatSection.STANDARD));
    }

    private static BigDecimal priceOf(List<SectionStats> sections, SeatSection section) {
        return sections.stream()
                .filter(stats -> stats.section() == section)
                .map(SectionStats::price)
                .findFirst()
                .orElse(null);
    }
}
