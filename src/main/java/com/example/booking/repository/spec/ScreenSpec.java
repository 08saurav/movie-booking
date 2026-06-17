package com.example.booking.repository.spec;

import com.example.booking.domain.Screen;
import org.springframework.data.jpa.domain.Specification;

public class ScreenSpec {

    public static Specification<Screen> theaterIdEq(Long theaterId) {
        return (root, q, cb) -> theaterId == null ? null : cb.equal(root.get("theater").get("id"), theaterId);
    }

    public static Specification<Screen> nameLike(String name) {
        return (root, q, cb) -> (name == null || name.isBlank()) ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
}
