package com.example.booking.repository;

import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowIdAndStatus(Long showId, ShowSeatStatus status);

    List<ShowSeat> findByShowId(Long showId);
}
