package com.example.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records the outcome of a booking attempt. Follows an explicit state machine:
 *   PAYMENT_PENDING → CONFIRMED (payment success, seat stays BOOKED)
 *   PAYMENT_PENDING → PAYMENT_FAILED (payment failure, seat released to AVAILABLE)
 *   CONFIRMED → CANCELLED (Segment 5)
 *
 * The UNIQUE constraint on show_seat_id is the hard backstop against
 * double-booking at the database level (complementing the atomic conditional
 * UPDATE from Segment 3 that serializes hold attempts).
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_seat_id", nullable = false)
    private ShowSeat showSeat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_tier_id", nullable = false)
    private PricingTier pricingTier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id")
    private DiscountCode discountCode;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Booking() {}

    public Booking(String customer, Show show, ShowSeat showSeat, PricingTier pricingTier,
                   DiscountCode discountCode, BigDecimal basePrice, BigDecimal discountAmount,
                   BigDecimal finalPrice, String idempotencyKey) {
        this.customer = customer;
        this.show = show;
        this.showSeat = showSeat;
        this.pricingTier = pricingTier;
        this.discountCode = discountCode;
        this.basePrice = basePrice;
        this.discountAmount = discountAmount;
        this.finalPrice = finalPrice;
        this.status = BookingStatus.PAYMENT_PENDING;
        this.idempotencyKey = idempotencyKey;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void confirm() {
        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void markPaymentFailed() {
        this.status = BookingStatus.PAYMENT_FAILED;
    }

    public void cancel(BigDecimal refundAmount) {
        this.status = BookingStatus.CANCELLED;
        this.refundAmount = refundAmount;
        this.cancelledAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCustomer() { return customer; }
    public Show getShow() { return show; }
    public ShowSeat getShowSeat() { return showSeat; }
    public PricingTier getPricingTier() { return pricingTier; }
    public DiscountCode getDiscountCode() { return discountCode; }
    public BigDecimal getBasePrice() { return basePrice; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public BookingStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public Instant getCancelledAt() { return cancelledAt; }
}
