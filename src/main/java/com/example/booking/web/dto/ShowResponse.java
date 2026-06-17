package com.example.booking.web.dto;

import com.example.booking.domain.RefundTier;
import com.example.booking.domain.Show;

import java.time.Instant;
import java.util.List;

public record ShowResponse(
        Long id,
        Long movieId,
        String movieTitle,
        Long screenId,
        String screenName,
        Long theaterId,
        String theaterName,
        Long cityId,
        String cityName,
        Instant startTime,
        Instant endTime,
        Long pricingTierId,
        String pricingTierName,
        Long refundPolicyId,
        String refundPolicyName,
        /** Tier ladder used to calculate refunds on cancellation; empty if no policy assigned. */
        List<RefundPolicyTierInfo> refundPolicyTiers,
        /** How many seats are still AVAILABLE right now. */
        long availableSeatCount
) {

    public record RefundPolicyTierInfo(int hoursBeforeShow, int refundPercent) {
        public static RefundPolicyTierInfo from(RefundTier tier) {
            return new RefundPolicyTierInfo(tier.getHoursBeforeShow(), tier.getRefundPercent());
        }
    }

    public static ShowResponse from(Show show, long availableSeatCount) {
        var screen = show.getScreen();
        var theater = screen.getTheater();
        var city = theater.getCity();
        var tier = show.getPricingTier();
        var policy = show.getRefundPolicy();
        List<RefundPolicyTierInfo> tiers = policy != null
                ? policy.getTiers().stream().map(RefundPolicyTierInfo::from).toList()
                : List.of();
        return new ShowResponse(
                show.getId(),
                show.getMovie().getId(),
                show.getMovie().getTitle(),
                screen.getId(),
                screen.getName(),
                theater.getId(),
                theater.getName(),
                city.getId(),
                city.getName(),
                show.getStartTime(),
                show.getEndTime(),
                tier != null ? tier.getId() : null,
                tier != null ? tier.getName() : null,
                policy != null ? policy.getId() : null,
                policy != null ? policy.getName() : null,
                tiers,
                availableSeatCount);
    }
}
