package com.seatsync.repository;

import com.seatsync.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Query("select distinct e.city from Event e where e.eventDate >= :from order by e.city")
    List<String> findCitiesWithEventsFrom(@Param("from") LocalDate from);

    long countByEventDateGreaterThanEqual(LocalDate date);
}
