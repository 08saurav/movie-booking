package com.example.booking.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin-configurable refund policy. Tiers are ordered highest-threshold-first
 * so the cancellation service can scan from most generous to least and apply
 * the first tier whose {@code hoursBeforeShow} is ≤ hours remaining.
 *
 * {@code isDefault = true} marks the fallback when a show has no explicit policy.
 * Only one policy should be marked as default; enforced in service logic.
 */
@Entity
@Table(name = "refund_policies")
public class RefundPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean defaultPolicy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "refund_policy_tiers", joinColumns = @JoinColumn(name = "policy_id"))
    @OrderBy("hoursBeforeShow DESC")
    private List<RefundTier> tiers = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefundPolicy() {}

    public RefundPolicy(String name, boolean defaultPolicy, List<RefundTier> tiers) {
        this.name = name;
        this.defaultPolicy = defaultPolicy;
        this.tiers = new ArrayList<>(tiers);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void updateName(String name) { this.name = name; }
    public void updateTiers(List<RefundTier> tiers) {
        this.tiers.clear();
        this.tiers.addAll(tiers);
    }
    public void setDefault(boolean defaultPolicy) { this.defaultPolicy = defaultPolicy; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isDefaultPolicy() { return defaultPolicy; }
    public List<RefundTier> getTiers() { return List.copyOf(tiers); }
    public Instant getCreatedAt() { return createdAt; }
}
