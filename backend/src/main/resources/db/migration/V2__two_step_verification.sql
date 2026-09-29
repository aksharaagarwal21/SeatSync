-- Two-step verification: one-time codes emailed for sign-in, registration, booking and cancellation.

ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
-- Accounts created before this migration are trusted as verified.
UPDATE users SET email_verified = TRUE;

CREATE TABLE otp_challenges (
    -- Random UUID: the challenge id is handed to unauthenticated clients during sign-in,
    -- so it must not be guessable or enumerable.
    id            UUID         PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose       VARCHAR(20)  NOT NULL,
    -- What the code approves, e.g. "event:12|seats:2251,2252". A code only works for that action.
    context       VARCHAR(500),
    -- Human-readable description of what is being approved, repeated in every email for this challenge.
    summary       VARCHAR(300) NOT NULL,
    -- HMAC-SHA256 of the code; the plain code is never stored.
    code_hash     VARCHAR(64)  NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    attempts      INT          NOT NULL DEFAULT 0,
    send_count    INT          NOT NULL DEFAULT 1,
    last_sent_at  TIMESTAMPTZ  NOT NULL,
    verified_at   TIMESTAMPTZ,
    consumed_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_otp_purpose CHECK (purpose IN ('LOGIN', 'REGISTRATION', 'BOOKING', 'CANCELLATION')),
    CONSTRAINT ck_otp_attempts CHECK (attempts >= 0 AND send_count >= 1)
);

-- Per-user rate limiting counts recent challenges.
CREATE INDEX ix_otp_user_created ON otp_challenges (user_id, created_at DESC);
-- Hourly cleanup of expired challenges.
CREATE INDEX ix_otp_expires ON otp_challenges (expires_at);
