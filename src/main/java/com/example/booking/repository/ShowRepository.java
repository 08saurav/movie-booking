package com.example.booking.repository;

import com.example.booking.domain.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    /** Upcoming shows (any screen) belonging to a given theater, soonest first. */
    List<Show> findByScreen_Theater_IdAndStartTimeAfterOrderByStartTimeAsc(Long theaterId, Instant after);
}
