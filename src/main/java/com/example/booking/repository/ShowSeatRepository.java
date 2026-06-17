package com.example.booking.repository;

import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowIdAndStatus(Long showId, ShowSeatStatus status);

    List<ShowSeat> findByShowId(Long showId);

    /**
     * Atomic conditional UPDATE that makes seat holds race-free. Exactly the logic
     * specified in Segment 3:
     * UPDATE show_seats SET status='HELD', held_by=:userId, hold_expires_at=:expiresAt
     * WHERE id=:seatId AND (status='AVAILABLE' OR (status='HELD' AND hold_expires_at < :now))
     *
     * Returns the number of rows updated: 1 if the hold succeeded, 0 if the seat
     * is taken. The database serializes concurrent attempts at the WHERE clause,
     * closing the TOCTOU window entirely. No application-level locking needed.
     *
     * @param seatId the show_seat id
     * @param userId the username/identifier of the customer holding the seat
     * @param expiresAt when this hold expires
     * @param now the current time, used to check if an existing hold is expired
     * @return 1 if hold succeeded, 0 if seat is taken
     */
    @Modifying
    @Query("""
        UPDATE ShowSeat s SET s.status = 'HELD', s.heldBy = :userId, s.holdExpiresAt = :expiresAt
        WHERE s.id = :seatId
          AND (s.status = 'AVAILABLE' OR (s.status = 'HELD' AND s.holdExpiresAt < :now))
        """)
    int holdSeat(
            @Param("seatId") Long seatId,
            @Param("userId") String userId,
            @Param("expiresAt") Instant expiresAt,
            @Param("now") Instant now);

    /**
     * Manually release a hold (customer releases early, or admin override).
     * Only succeeds if the seat is currently HELD and held by the given user.
     *
     * @return 1 if released, 0 if not held or held by someone else
     */
    @Modifying
    @Query("""
        UPDATE ShowSeat s SET s.status = 'AVAILABLE', s.heldBy = null, s.holdExpiresAt = null
        WHERE s.id = :seatId AND s.status = 'HELD' AND s.heldBy = :userId
        """)
    int releaseSeat(
            @Param("seatId") Long seatId,
            @Param("userId") String userId);

    /**
     * Find expired HELD seats (sweeper). Runs periodically to flip expired
     * HELD seats back to AVAILABLE, keeping state clean. Lazy expiry in the
     * hold UPDATE WHERE clause ensures correctness; this sweeper handles
     * clean state for accurate reporting.
     */
    @Query("""
        SELECT s FROM ShowSeat s
        WHERE s.status = 'HELD' AND s.holdExpiresAt < :now
        """)
    List<ShowSeat> findExpiredHolds(@Param("now") Instant now);

    /**
     * Release all expired holds (sweep cleanup).
     */
    @Modifying
    @Query("""
        UPDATE ShowSeat s SET s.status = 'AVAILABLE', s.heldBy = null, s.holdExpiresAt = null
        WHERE s.status = 'HELD' AND s.holdExpiresAt < :now
        """)
    int releaseAllExpiredHolds(@Param("now") Instant now);

    /**
     * Atomically transition a HELD seat to BOOKED (used when initiating payment).
     * Moving to BOOKED before payment prevents the hold-expiry sweeper from
     * reclaiming the seat during payment processing.
     *
     * @return 1 if transitioned, 0 if seat was not HELD by this user (concurrent modification)
     */
    @Modifying
    @Query("""
        UPDATE ShowSeat s SET s.status = 'BOOKED', s.heldBy = null, s.holdExpiresAt = null
        WHERE s.id = :seatId AND s.status = 'HELD' AND s.heldBy = :userId
        """)
    int bookSeat(@Param("seatId") Long seatId, @Param("userId") String userId);

    /**
     * Release a BOOKED seat back to AVAILABLE. Used on payment failure so the
     * seat re-enters the inventory for other customers to book.
     *
     * @return 1 if released, 0 if seat was not BOOKED (should not happen in normal flow)
     */
    @Modifying
    @Query("""
        UPDATE ShowSeat s SET s.status = 'AVAILABLE', s.heldBy = null, s.holdExpiresAt = null
        WHERE s.id = :seatId AND s.status = 'BOOKED'
        """)
    int releaseBookedSeat(@Param("seatId") Long seatId);

    Optional<ShowSeat> findByShowIdAndSeatId(Long showId, Long seatId);
}
