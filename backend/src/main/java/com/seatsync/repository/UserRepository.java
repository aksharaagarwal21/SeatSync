package com.seatsync.repository;

import com.seatsync.dto.admin.AdminUserResponse;
import com.seatsync.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query(value = """
            select new com.seatsync.dto.admin.AdminUserResponse(
                u.id, u.name, u.email, u.role, u.createdAt,
                (select count(b) from Booking b
                  where b.user = u and b.status = com.seatsync.entity.BookingStatus.CONFIRMED))
            from User u
            order by u.createdAt desc, u.id desc
            """,
            countQuery = "select count(u) from User u")
    Page<AdminUserResponse> findUserSummaries(Pageable pageable);
}
