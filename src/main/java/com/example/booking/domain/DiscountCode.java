package com.example.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A promotional discount code. {@code maxUses == null} means unlimited.
 * Use-count increment happens inside the booking transaction; concurrent
 * applications are serialized via a pessimistic write lock in
 * {@code DiscountCodeRepository.findByCodeForUpdate}.
 */
@Entity
@Table(name = "discount_codes")
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    private int currentUses;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DiscountCode() {}

    public DiscountCode(String code, DiscountType discountType, BigDecimal value,
                        Integer maxUses, Instant validFrom, Instant validTo, boolean active) {
        this.code = code;
        this.discountType = discountType;
        this.value = value;
        this.maxUses = maxUses;
        this.currentUses = 0;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = active;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void incrementUses() {
        this.currentUses++;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public DiscountType getDiscountType() { return discountType; }
    public BigDecimal getValue() { return value; }
    public Integer getMaxUses() { return maxUses; }
    public int getCurrentUses() { return currentUses; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidTo() { return validTo; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
