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
 * {@code totalRows}/{@code totalCols} are fixed at creation: the seat layout
 * is generated once (see {@code ScreenService}) and is not regenerated on
 * update, so changing the dimensions after the fact would desync this field
 * from the actual {@link Seat} rows. Renaming a screen is allowed; resizing
 * is not exposed via the update endpoint.
 */
@Entity
@Table(name = "screens")
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "total_cols", nullable = false)
    private int totalCols;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Screen() {
        // JPA
    }

    public Screen(Theater theater, String name, int totalRows, int totalCols) {
        this.theater = theater;
        this.name = name;
        this.totalRows = totalRows;
        this.totalCols = totalCols;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Theater getTheater() {
        return theater;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getTotalCols() {
        return totalCols;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
