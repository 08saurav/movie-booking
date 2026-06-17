package com.example.booking.repository.spec;

import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import org.springframework.data.jpa.domain.Specification;

public class ShowSeatSpec {

    public static Specification<ShowSeat> showIdEq(Long showId) {
        return (root, q, cb) -> cb.equal(root.get("show").get("id"), showId);
    }

    public static Specification<ShowSeat> statusEq(ShowSeatStatus status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<ShowSeat> categoryEq(SeatCategory category) {
        return (root, q, cb) -> category == null ? null : cb.equal(root.get("seat").get("category"), category);
    }

    public static Specification<ShowSeat> rowLabelEq(String rowLabel) {
        return (root, q, cb) -> (rowLabel == null || rowLabel.isBlank()) ? null
                : cb.equal(cb.upper(root.get("seat").get("rowLabel")), rowLabel.toUpperCase());
    }
}
