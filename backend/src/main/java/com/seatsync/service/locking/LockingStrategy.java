package com.seatsync.service.locking;

/**
 * OPTIMISTIC: read seats without locks and rely on the {@code @Version} check at flush. Cheap when
 * contention is low; losers fail fast with a version conflict.
 * <p>
 * PESSIMISTIC: {@code SELECT ... FOR UPDATE} in ascending id order. Concurrent requests for the same
 * seats queue on the row lock; the winner commits and everyone behind it sees the seat as BOOKED.
 */
public enum LockingStrategy {
    OPTIMISTIC,
    PESSIMISTIC
}
