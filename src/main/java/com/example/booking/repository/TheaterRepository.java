package com.example.booking.repository;

import com.example.booking.domain.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TheaterRepository extends JpaRepository<Theater, Long>, JpaSpecificationExecutor<Theater> {

    List<Theater> findByCityId(Long cityId);
}
