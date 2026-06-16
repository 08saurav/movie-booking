package com.example.booking.repository;

import com.example.booking.domain.Theater;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TheaterRepository extends JpaRepository<Theater, Long> {

    List<Theater> findByCityId(Long cityId);
}
