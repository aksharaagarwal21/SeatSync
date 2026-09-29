package com.seatsync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(name = "row_label", nullable = false, length = 2)
    private String row;

    @Column(name = "seat_index", nullable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatSection section;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    @Column(name = "held_by_user_id")
    private Long heldByUserId;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    /**
     * Optimistic lock. Every state change bumps the version; a concurrent transaction that
     * read an older version fails its UPDATE ... WHERE version = ? and is rejected.
     */
    @Version
    @Column(nullable = false)
    private long version;

    protected Seat() {
    }

    public Seat(Event event, String row, int number, SeatSection section, BigDecimal price) {
        this.event = event;
        this.row = row;
        this.number = number;
        this.seatNumber = row + number;
        this.section = section;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    /**
     * A seat can be taken by {@code userId} when it is free, when its hold has lapsed,
     * or when the same user is holding it.
     */
    public boolean isAvailableTo(Long userId, Instant now) {
        return switch (status) {
            case AVAILABLE -> true;
            case RESERVED -> isHoldExpired(now) || Objects.equals(heldByUserId, userId);
            case BOOKED -> false;
        };
    }

    public boolean isHeldBy(Long userId, Instant now) {
        return status == SeatStatus.RESERVED && Objects.equals(heldByUserId, userId) && !isHoldExpired(now);
    }

    /** Status as other users should see it: lapsed holds are shown as available. */
    public SeatStatus effectiveStatus(Instant now) {
        return status == SeatStatus.RESERVED && isHoldExpired(now) ? SeatStatus.AVAILABLE : status;
    }

    public void hold(Long userId, Instant expiresAt) {
        this.status = SeatStatus.RESERVED;
        this.heldByUserId = userId;
        this.holdExpiresAt = expiresAt;
    }

    public void book() {
        this.status = SeatStatus.BOOKED;
        clearHold();
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
        clearHold();
    }

    private boolean isHoldExpired(Instant now) {
        return holdExpiresAt == null || !holdExpiresAt.isAfter(now);
    }

    private void clearHold() {
        this.heldByUserId = null;
        this.holdExpiresAt = null;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getRow() {
        return row;
    }

    public int getNumber() {
        return number;
    }

    public SeatSection getSection() {
        return section;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public Long getHeldByUserId() {
        return heldByUserId;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public long getVersion() {
        return version;
    }
}
