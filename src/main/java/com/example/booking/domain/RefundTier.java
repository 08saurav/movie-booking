package com.example.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * One row in a refund policy's tier ladder. At cancellation time, the tier
 * with the largest {@code hoursBeforeShow} that is still ≤ hours remaining
 * until the show start is applied.
 *
 * Example: tiers at 24h→100%, 12h→50%, 0h→0%. If cancelled 18 hours before
 * showtime, the 12h tier applies → 50% refund.
 */
@Embeddable
public class RefundTier {

    @Column(name = "hours_before_show", nullable = false)
    private int hoursBeforeShow;

    @Column(name = "refund_percent", nullable = false)
    private int refundPercent;

    protected RefundTier() {}

    public RefundTier(int hoursBeforeShow, int refundPercent) {
        this.hoursBeforeShow = hoursBeforeShow;
        this.refundPercent = refundPercent;
    }

    public int getHoursBeforeShow() { return hoursBeforeShow; }
    public int getRefundPercent() { return refundPercent; }
}
