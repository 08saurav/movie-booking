package com.example.booking.web.dto;

import com.example.booking.domain.Show;

import java.time.Instant;

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
        String pricingTierName
) {

    public static ShowResponse from(Show show) {
        var screen = show.getScreen();
        var theater = screen.getTheater();
        var city = theater.getCity();
        var tier = show.getPricingTier();
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
                tier != null ? tier.getName() : null);
    }
}
