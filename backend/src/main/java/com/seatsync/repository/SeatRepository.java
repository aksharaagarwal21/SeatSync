package com.seatsync.repository;

import com.seatsync.entity.Seat;
import com.seatsync.entity.SeatSection;
import com.seatsync.entity.SeatStatus;
import com.seatsync.repository.projection.EventSeatStats;
import com.seatsync.repository.projection.SectionStats;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("select s from Seat s where s.event.id = :eventId order by s.row, s.number")
    List<Seat> findSeatMap(@Param("eventId") Long eventId);

    /**
     * Plain read used by the optimistic strategy. Conflicts are detected later by the
     * {@code @Version} check when the modified seats are flushed.
     */
    @Query("select s from Seat s where s.event.id = :eventId and s.id in :seatIds order by s.id")
    List<Seat> findForBooking(@Param("eventId") Long eventId, @Param("seatIds") Collection<Long> seatIds);

    /**
     * SELECT ... FOR UPDATE used by the pessimistic strategy. Rows are locked in ascending id
     * order so two transactions requesting overlapping seats always queue instead of deadlocking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.event.id = :eventId and s.id in :seatIds order by s.id")
    List<Seat> lockForBooking(@Param("eventId") Long eventId, @Param("seatIds") Collection<Long> seatIds);

    @Query("""
            select s.id from Seat s
            where s.event.id = :eventId
              and s.status = com.seatsync.entity.SeatStatus.RESERVED
              and s.heldByUserId = :userId
            """)
    List<Long> findIdsHeldBy(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Query("""
            select new com.seatsync.repository.projection.EventSeatStats(
                s.event.id,
                count(s),
                sum(case when s.status = com.seatsync.entity.SeatStatus.AVAILABLE then 1 else 0 end),
                sum(case when s.status = com.seatsync.entity.SeatStatus.RESERVED then 1 else 0 end),
                sum(case when s.status = com.seatsync.entity.SeatStatus.BOOKED then 1 else 0 end),
                min(s.price),
                max(s.price))
            from Seat s
            where s.event.id in :eventIds
            group by s.event.id
            """)
    List<EventSeatStats> findStatsByEventIds(@Param("eventIds") Collection<Long> eventIds);

    @Query("""
            select new com.seatsync.repository.projection.SectionStats(
                s.section,
                min(s.price),
                count(s),
                sum(case when s.status = com.seatsync.entity.SeatStatus.AVAILABLE then 1 else 0 end))
            from Seat s
            where s.event.id = :eventId
            group by s.section
            """)
    List<SectionStats> findSectionStats(@Param("eventId") Long eventId);

    @Query("""
            select count(s) from Seat s
            where s.status = com.seatsync.entity.SeatStatus.AVAILABLE and s.event.eventDate >= :from
            """)
    long countAvailableFrom(@Param("from") LocalDate from);

    boolean existsByEventIdAndStatusNot(Long eventId, SeatStatus status);

    @Query("""
            select distinct s.event.id from Seat s
            where s.status = com.seatsync.entity.SeatStatus.RESERVED and s.holdExpiresAt <= :now
            """)
    List<Long> findEventIdsWithExpiredHolds(@Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Seat s
               set s.status = com.seatsync.entity.SeatStatus.AVAILABLE,
                   s.heldByUserId = null,
                   s.holdExpiresAt = null,
                   s.version = s.version + 1
             where s.status = com.seatsync.entity.SeatStatus.RESERVED and s.holdExpiresAt <= :now
            """)
    int releaseExpiredHolds(@Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Seat s
               set s.price = :price, s.version = s.version + 1
             where s.event.id = :eventId and s.section = :section and s.price <> :price
            """)
    int updateSectionPrice(@Param("eventId") Long eventId,
                           @Param("section") SeatSection section,
                           @Param("price") BigDecimal price);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Seat s where s.event.id = :eventId")
    int deleteByEventId(@Param("eventId") Long eventId);
}
