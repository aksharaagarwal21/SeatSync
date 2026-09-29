package com.seatsync.repository;

import com.seatsync.entity.OtpChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    /** Row lock: parallel guesses against one challenge are checked one at a time, so the attempt limit holds. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "user")
    @Query("select c from OtpChallenge c where c.id = :id")
    Optional<OtpChallenge> lockById(@Param("id") UUID id);

    long countByUserIdAndCreatedAtAfter(Long userId, Instant since);

    /** Atomic single use: of two concurrent requests with the same code, only one updates the row. */
    @Modifying(flushAutomatically = true)
    @Query("update OtpChallenge c set c.consumedAt = :now where c.id = :id and c.consumedAt is null")
    int consume(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("delete from OtpChallenge c where c.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
