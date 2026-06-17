package com.example.booking.service;

import com.example.booking.repository.ShowSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled task that periodically sweeps expired seat holds and flips them
 * back to AVAILABLE. Runs every minute.
 *
 * Correctness is guaranteed by lazy expiry in the hold UPDATE WHERE clause
 * (Segment 3): no concurrent thread can acquire an expired hold. This sweeper
 * handles clean state for accurate reporting.
 *
 * Segment 3: Concurrency Core.
 */
@Component
public class HoldExpirySweeperTask {

    private static final Logger log = LoggerFactory.getLogger(HoldExpirySweeperTask.class);

    private final ShowSeatRepository showSeatRepository;

    public HoldExpirySweeperTask(ShowSeatRepository showSeatRepository) {
        this.showSeatRepository = showSeatRepository;
    }

    /**
     * Run every minute to clean up expired holds. Uses @Transactional to ensure
     * the batch UPDATE is properly committed.
     */
    @Scheduled(fixedRate = 60_000) // 60 seconds
    @Transactional
    public void sweepExpiredHolds() {
        Instant now = Instant.now();
        int released = showSeatRepository.releaseAllExpiredHolds(now);
        if (released > 0) {
            log.debug("Hold expiry sweeper: released {} expired holds", released);
        }
    }
}
