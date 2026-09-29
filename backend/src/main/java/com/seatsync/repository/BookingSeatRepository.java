package com.seatsync.repository;

import com.seatsync.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    long countByActiveTrue();
}
