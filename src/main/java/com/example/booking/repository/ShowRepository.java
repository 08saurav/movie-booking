package com.example.booking.repository;

import com.example.booking.domain.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long>, JpaSpecificationExecutor<Show> {

    /** Upcoming shows (any screen) belonging to a given theater, soonest first. */
    List<Show> findByScreen_Theater_IdAndStartTimeAfterOrderByStartTimeAsc(Long theaterId, Instant after);
}
