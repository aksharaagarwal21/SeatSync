package com.seatsync.repository;

import com.seatsync.entity.Booking;
import com.seatsync.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    /** Seats and their lines are loaded afterwards in batches (hibernate.default_batch_fetch_size). */
    @EntityGraph(attributePaths = "event")
    Page<Booking> findByUserIdOrderByBookingTimeDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "user"})
    Optional<Booking> findWithDetailsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"event", "user"})
    Page<Booking> findAll(Specification<Booking> specification, Pageable pageable);

    boolean existsByEventId(Long eventId);

    long countByStatus(BookingStatus status);

    /**
     * Confirmed bookings and tickets per local calendar day. The day is formatted in SQL so the
     * result does not depend on the JDBC driver's date mapping.
     */
    @Query(value = """
            select to_char(b.booking_time at time zone :zone, 'YYYY-MM-DD') as day,
                   count(distinct b.id)                                 as bookings,
                   count(bs.id)                                         as tickets
              from bookings b
              join booking_seats bs on bs.booking_id = b.id
             where b.status = 'CONFIRMED' and b.booking_time >= :from
             group by day
             order by day
            """, nativeQuery = true)
    List<Object[]> countDailyConfirmed(@Param("from") Instant from, @Param("zone") String zone);
}
