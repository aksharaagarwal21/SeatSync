package com.seatsync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventCategory category;

    @Column(nullable = false, length = 150)
    private String venue;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "seats_per_row", nullable = false)
    private int seatsPerRow;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Event() {
    }

    public Event(EventDetails details, EventStatus status, int rowCount, int seatsPerRow, Instant createdAt) {
        applyDetails(details);
        this.status = status;
        this.rowCount = rowCount;
        this.seatsPerRow = seatsPerRow;
        this.createdAt = createdAt;
    }

    public void update(EventDetails details, EventStatus status) {
        applyDetails(details);
        this.status = status;
    }

    public void changeLayout(int rowCount, int seatsPerRow) {
        this.rowCount = rowCount;
        this.seatsPerRow = seatsPerRow;
    }

    public boolean hasLayout(int rowCount, int seatsPerRow) {
        return this.rowCount == rowCount && this.seatsPerRow == seatsPerRow;
    }

    public Instant startsAt(ZoneId zone) {
        return eventDate.atTime(startTime).atZone(zone).toInstant();
    }

    public boolean isBookable(Instant now, ZoneId zone) {
        return status == EventStatus.ON_SALE && startsAt(zone).isAfter(now);
    }

    private void applyDetails(EventDetails details) {
        this.name = details.name();
        this.description = details.description();
        this.category = details.category();
        this.venue = details.venue();
        this.city = details.city();
        this.eventDate = details.eventDate();
        this.startTime = details.startTime();
        this.imageUrl = details.imageUrl();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public String getVenue() {
        return venue;
    }

    public String getCity() {
        return city;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public EventStatus getStatus() {
        return status;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getSeatsPerRow() {
        return seatsPerRow;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
