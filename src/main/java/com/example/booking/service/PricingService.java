package com.example.booking.service;

import com.example.booking.domain.DiscountCode;
import com.example.booking.domain.DiscountType;
import com.example.booking.domain.PricingTier;
import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.Show;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.ZoneOffset;

/**
 * Price calculation formula:
 *   base_price × weekend_multiplier (if show is on Sat/Sun in UTC) × (1 - discount) = final_price
 *
 * Weekend is determined by the show's start_time in UTC, which is the stored
 * canonical timezone. Flat discounts are capped at the price to prevent negative totals.
 */
@Service
public class PricingService {

    public PriceBreakdown calculate(PricingTier tier, SeatCategory category, Show show, DiscountCode discountCode) {
        BigDecimal base = category == SeatCategory.PREMIUM
                ? tier.getPremiumPrice()
                : tier.getRegularPrice();

        DayOfWeek day = show.getStartTime().atZone(ZoneOffset.UTC).getDayOfWeek();
        boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        BigDecimal afterWeekend = isWeekend
                ? base.multiply(tier.getWeekendMultiplier()).setScale(2, RoundingMode.HALF_UP)
                : base.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (discountCode != null) {
            if (discountCode.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = afterWeekend
                        .multiply(discountCode.getValue().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                discountAmount = discountCode.getValue().min(afterWeekend).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal finalPrice = afterWeekend.subtract(discountAmount).max(BigDecimal.ZERO);
        return new PriceBreakdown(base.setScale(2, RoundingMode.HALF_UP), discountAmount, finalPrice);
    }

    public record PriceBreakdown(BigDecimal basePrice, BigDecimal discountAmount, BigDecimal finalPrice) {}
}
