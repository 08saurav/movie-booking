package com.example.booking.repository.spec;

import com.example.booking.domain.DiscountCode;
import com.example.booking.domain.DiscountType;
import org.springframework.data.jpa.domain.Specification;

public class DiscountCodeSpec {

    public static Specification<DiscountCode> activeEq(Boolean active) {
        return (root, q, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }

    public static Specification<DiscountCode> discountTypeEq(DiscountType discountType) {
        return (root, q, cb) -> discountType == null ? null : cb.equal(root.get("discountType"), discountType);
    }

    public static Specification<DiscountCode> codeLike(String code) {
        return (root, q, cb) -> (code == null || code.isBlank()) ? null
                : cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
    }
}
