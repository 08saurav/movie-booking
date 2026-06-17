package com.example.booking.repository.spec;

import com.example.booking.domain.City;
import org.springframework.data.jpa.domain.Specification;

public class CitySpec {

    public static Specification<City> nameLike(String name) {
        return (root, q, cb) -> (name == null || name.isBlank()) ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<City> stateEq(String state) {
        return (root, q, cb) -> state == null ? null : cb.equal(root.get("state"), state);
    }

    public static Specification<City> countryEq(String country) {
        return (root, q, cb) -> country == null ? null : cb.equal(root.get("country"), country);
    }
}
