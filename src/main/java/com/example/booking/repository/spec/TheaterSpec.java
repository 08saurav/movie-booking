package com.example.booking.repository.spec;

import com.example.booking.domain.Theater;
import org.springframework.data.jpa.domain.Specification;

public class TheaterSpec {

    public static Specification<Theater> cityIdEq(Long cityId) {
        return (root, q, cb) -> cityId == null ? null : cb.equal(root.get("city").get("id"), cityId);
    }

    public static Specification<Theater> nameLike(String name) {
        return (root, q, cb) -> (name == null || name.isBlank()) ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
}
