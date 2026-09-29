package com.seatsync.repository;

import com.seatsync.entity.Booking;
import com.seatsync.entity.BookingStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<Booking> matching(Long eventId, BookingStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (eventId != null) {
                predicates.add(cb.equal(root.get("event").get("id"), eventId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
