package com.example.booking.service;

import com.example.booking.domain.Show;
import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.exception.SeatNotAvailableException;
import com.example.booking.repository.ShowRepository;
import com.example.booking.repository.ShowSeatRepository;
import com.example.booking.web.dto.SeatHoldResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Manages seat holds: atomic conditional UPDATE to prevent races, hold expiry
 * tracking, and manual release. The hold mechanism is race-free at the database
 * level (atomic UPDATE in the WHERE clause); no application-level locking.
 *
 * Segment 3: Concurrency Core.
 */
@Service
@Transactional
public class SeatHoldService {

    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;

    @Value("${booking.seat-hold-ttl-minutes:10}")
    private int holdTtlMinutes;

    public SeatHoldService(ShowSeatRepository showSeatRepository, ShowRepository showRepository) {
        this.showSeatRepository = showSeatRepository;
        this.showRepository = showRepository;
    }

    /**
     * Hold a seat atomically. Fails (409) if the seat is already booked or
     * held by someone else (and not expired). Uses an atomic conditional UPDATE
     * so the database serializes concurrent attempts.
     */
    public SeatHoldResponse holdSeat(Long showId, Long seatId, String userId) {
        // Validate the show exists
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show " + showId + " not found"));

        ShowSeat showSeat = showSeatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("ShowSeat " + seatId + " not found"));

        if (!showSeat.getShow().getId().equals(showId)) {
            throw new IllegalArgumentException("Seat does not belong to this show");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(holdTtlMinutes * 60L);

        // Atomic conditional UPDATE: succeeds only if seat is AVAILABLE or hold is expired
        int updated = showSeatRepository.holdSeat(seatId, userId, expiresAt, now);

        if (updated == 0) {
            // Hold failed: seat is taken, held by someone else, or in a non-holdable state
            throw new SeatNotAvailableException("Seat " + seatId + " is not available");
        }

        // Refresh to get updated entity
        ShowSeat heldSeat = showSeatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("ShowSeat " + seatId + " not found"));

        return SeatHoldResponse.from(heldSeat);
    }

    /**
     * Release a hold early (customer cancels the hold before booking).
     * Only succeeds if the customer holds the seat.
     */
    public void releaseSeat(Long seatId, String userId) {
        ShowSeat showSeat = showSeatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("ShowSeat " + seatId + " not found"));

        if (showSeat.getStatus() != ShowSeatStatus.HELD) {
            throw new IllegalStateException("Seat " + seatId + " is not currently held");
        }

        if (!userId.equals(showSeat.getHeldBy())) {
            throw new IllegalStateException("Seat is held by a different customer");
        }

        int released = showSeatRepository.releaseSeat(seatId, userId);
        if (released == 0) {
            throw new IllegalStateException("Failed to release hold on seat " + seatId);
        }
    }
}
