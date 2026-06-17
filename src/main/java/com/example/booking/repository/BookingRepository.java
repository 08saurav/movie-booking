package com.example.booking.repository;

import com.example.booking.domain.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerOrderByCreatedAtDesc(String customer);

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    Page<Booking> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
