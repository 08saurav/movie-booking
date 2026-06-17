package com.example.booking.service;

import com.example.booking.domain.Show;
import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import com.example.booking.exception.InvalidRequestException;
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
 * Segment 7: Improvements — added show-started guard, one-hold-per-show
 * enforcement, and idempotent re-hold (refreshes expiry instead of 409).
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
     * Hold a seat atomically. Business rules enforced before the DB update:
     * <ul>
     *   <li>Show must not have already started.</li>
     *   <li>Customer may hold at most one seat per show (prevents seat-squatting).</li>
     *   <li>Re-holding the same seat the customer already holds refreshes the
     *       expiry instead of returning 409, making the call idempotent.</li>
     * </ul>
     */
    public SeatHoldResponse holdSeat(Long showId, Long seatId, String userId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show " + showId + " not found"));

        if (!show.getStartTime().isAfter(Instant.now())) {
            throw new InvalidRequestException("Show has already started — seats can no longer be held");
        }

        ShowSeat showSeat = showSeatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("ShowSeat " + seatId + " not found"));

        if (!showSeat.getShow().getId().equals(showId)) {
            throw new InvalidRequestException("Seat does not belong to this show");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(holdTtlMinutes * 60L);

        // One-hold-per-show: reject if the customer actively holds a *different* seat
        long otherActiveHolds = showSeatRepository.countActiveHoldsForUserExcludingSeat(showId, userId, now, seatId);
        if (otherActiveHolds > 0) {
            throw new InvalidRequestException(
                    "You already have an active hold for this show. Release it before holding another seat.");
        }

        // Atomic conditional UPDATE: succeeds if the seat is AVAILABLE or its hold is expired
        int updated = showSeatRepository.holdSeat(seatId, userId, expiresAt, now);

        if (updated == 0) {
            // Seat wasn't available — check if we're the one holding it (idempotent re-hold)
            int refreshed = showSeatRepository.refreshHold(seatId, userId, expiresAt);
            if (refreshed == 0) {
                throw new SeatNotAvailableException("Seat " + seatId + " is not available");
            }
        }

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
