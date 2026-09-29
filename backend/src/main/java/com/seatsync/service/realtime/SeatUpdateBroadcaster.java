package com.seatsync.service.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pushes seat changes to browsers watching an event's seat map over Server-Sent Events.
 * Messages only say "these seats changed"; clients refetch the seat map so per-user fields
 * (such as heldByMe) stay correct. Subscriptions are per instance; clients also poll as a fallback.
 */
@Component
public class SeatUpdateBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(SeatUpdateBroadcaster.class);
    private static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, Set<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long eventId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        subscribers.computeIfAbsent(eventId, id -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> unsubscribe(eventId, emitter));
        emitter.onTimeout(() -> unsubscribe(eventId, emitter));
        emitter.onError(error -> unsubscribe(eventId, emitter));
        send(eventId, emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSeatsChanged(SeatsChangedEvent change) {
        Set<SseEmitter> emitters = subscribers.get(change.eventId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        var payload = Map.of("eventId", change.eventId(), "seatIds", change.seatIds());
        emitters.forEach(emitter -> send(change.eventId(), emitter, SseEmitter.event().name("seats").data(payload)));
    }

    /** Keeps idle connections alive through proxies and prunes clients that went away. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        subscribers.forEach((eventId, emitters) ->
                emitters.forEach(emitter -> send(eventId, emitter, SseEmitter.event().comment("ping"))));
    }

    private void send(Long eventId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException ex) {
            log.debug("Dropping seat-stream subscriber for event {}: {}", eventId, ex.getMessage());
            unsubscribe(eventId, emitter);
        }
    }

    private void unsubscribe(Long eventId, SseEmitter emitter) {
        subscribers.computeIfPresent(eventId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
