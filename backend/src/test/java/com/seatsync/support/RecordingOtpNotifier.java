package com.seatsync.support;

import com.seatsync.entity.OtpPurpose;
import com.seatsync.entity.User;
import com.seatsync.service.verification.OtpNotifier;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Stands in for email in tests: remembers the last code sent to each address. */
public class RecordingOtpNotifier implements OtpNotifier {

    private final Map<String, String> lastCodeByEmail = new ConcurrentHashMap<>();
    private final Map<String, String> lastSummaryByEmail = new ConcurrentHashMap<>();
    private final AtomicInteger sent = new AtomicInteger();

    @Override
    public void send(User user, OtpPurpose purpose, String code, String summary, Duration validFor) {
        lastCodeByEmail.put(user.getEmail(), code);
        lastSummaryByEmail.put(user.getEmail(), summary);
        sent.incrementAndGet();
    }

    public String lastCodeFor(String email) {
        String code = lastCodeByEmail.get(email);
        if (code == null) {
            throw new IllegalStateException("No code was sent to " + email);
        }
        return code;
    }

    public String lastSummaryFor(String email) {
        return lastSummaryByEmail.get(email);
    }

    public int sentCount() {
        return sent.get();
    }

    public void reset() {
        lastCodeByEmail.clear();
        lastSummaryByEmail.clear();
        sent.set(0);
    }
}
