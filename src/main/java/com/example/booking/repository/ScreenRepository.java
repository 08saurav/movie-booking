package com.example.booking.repository;

import com.example.booking.domain.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Long>, JpaSpecificationExecutor<Screen> {

    List<Screen> findByTheaterId(Long theaterId);
}
