package com.seatsync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A one-time code sent to a user's email to approve one specific action. */
@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(length = 500)
    private String context;

    @Column(nullable = false, length = 300)
    private String summary;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "send_count", nullable = false)
    private int sendCount;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OtpChallenge() {
    }

    public OtpChallenge(User user, OtpPurpose purpose, String context, String summary, String codeHash, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.purpose = purpose;
        this.context = context;
        this.summary = summary;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.sendCount = 1;
        this.lastSentAt = now;
        this.createdAt = now;
    }

    /** A new code replaces the old one; the attempt counter starts over. */
    public void resend(String newCodeHash, Instant now, Instant newExpiry) {
        this.codeHash = newCodeHash;
        this.expiresAt = newExpiry;
        this.attempts = 0;
        this.sendCount++;
        this.lastSentAt = now;
    }

    public void recordFailedAttempt() {
        this.attempts++;
    }

    public void markVerified(Instant now) {
        if (verifiedAt == null) {
            this.verifiedAt = now;
        }
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean matches(OtpPurpose expectedPurpose, String expectedContext) {
        return purpose == expectedPurpose && Objects.equals(context, expectedContext);
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public String getSummary() {
        return summary;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getSendCount() {
        return sendCount;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }
}
