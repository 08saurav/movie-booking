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
import jakarta.persistence.Table;

/**
 * Never created directly via a request DTO: rows are generated in bulk by
 * {@code ScreenService} when a screen is created, one per (row, column).
 */
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "row_label", nullable = false, length = 2)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatCategory category;

    protected Seat() {
        // JPA
    }

    public Seat(Screen screen, String rowLabel, int seatNumber, SeatCategory category) {
        this.screen = screen;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public Screen getScreen() {
        return screen;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public SeatCategory getCategory() {
        return category;
    }
}
