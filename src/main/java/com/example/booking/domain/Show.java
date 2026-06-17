package com.example.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;


/**
 * {@code movie}/{@code screen} are fixed at creation: a show's per-show seat
 * inventory ({@link ShowSeat}) is generated once from the screen's seats, so
 * changing the screen afterward would desync the inventory. Only
 * {@code startTime} (and the {@code endTime} derived from it) is mutable via
 * the update endpoint; rescheduling to a different movie/screen means
 * deleting and recreating the show.
 */
@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_tier_id")
    private PricingTier pricingTier;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Show() {
        // JPA
    }

    public Show(Movie movie, Screen screen, Instant startTime, Instant endTime) {
        this(movie, screen, startTime, endTime, null);
    }

    public Show(Movie movie, Screen screen, Instant startTime, Instant endTime, PricingTier pricingTier) {
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pricingTier = pricingTier;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Movie getMovie() {
        return movie;
    }

    public Screen getScreen() {
        return screen;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public PricingTier getPricingTier() {
        return pricingTier;
    }

    public void assignPricingTier(PricingTier tier) {
        this.pricingTier = tier;
    }

    public void reschedule(Instant startTime, Instant endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
