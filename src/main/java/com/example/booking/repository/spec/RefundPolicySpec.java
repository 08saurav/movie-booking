package com.example.booking.repository.spec;

import com.example.booking.domain.RefundPolicy;
import org.springframework.data.jpa.domain.Specification;

public class RefundPolicySpec {

    public static Specification<RefundPolicy> isDefault(Boolean isDefault) {
        return (root, q, cb) -> isDefault == null ? null : cb.equal(root.get("defaultPolicy"), isDefault);
    }

    public static Specification<RefundPolicy> nameLike(String name) {
        return (root, q, cb) -> (name == null || name.isBlank()) ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
}
