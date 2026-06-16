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
 * Modeled as a unidirectional {@code @ManyToOne} to {@link City} only -- no
 * inverse {@code City.theaters} collection. Child lists are fetched via
 * repository queries (e.g. {@code findByCityId}) instead of navigating
 * bidirectional JPA associations, which avoids the N+1/stale-collection
 * pitfalls those associations are prone to. The same choice is repeated for
 * every other parent/child pair in this domain model.
 */
@Entity
@Table(name = "theaters")
public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false)
    private String name;

    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Theater() {
        // JPA
    }

    public Theater(City city, String name, String address) {
        this.city = city;
        this.name = name;
        this.address = address;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
