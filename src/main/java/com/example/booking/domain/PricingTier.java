package com.example.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Defines base prices per seat category and a weekend multiplier.
 * Linked optionally to shows; a show without a pricing tier cannot be booked.
 */
@Entity
@Table(name = "pricing_tiers")
public class PricingTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "regular_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal regularPrice;

    @Column(name = "premium_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumPrice;

    @Column(name = "weekend_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal weekendMultiplier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PricingTier() {}

    public PricingTier(String name, BigDecimal regularPrice, BigDecimal premiumPrice, BigDecimal weekendMultiplier) {
        this.name = name;
        this.regularPrice = regularPrice;
        this.premiumPrice = premiumPrice;
        this.weekendMultiplier = weekendMultiplier;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getRegularPrice() { return regularPrice; }
    public BigDecimal getPremiumPrice() { return premiumPrice; }
    public BigDecimal getWeekendMultiplier() { return weekendMultiplier; }
    public Instant getCreatedAt() { return createdAt; }
}
