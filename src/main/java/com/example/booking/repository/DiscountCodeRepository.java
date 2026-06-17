package com.example.booking.repository;

import com.example.booking.domain.DiscountCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long>, JpaSpecificationExecutor<DiscountCode> {

    Optional<DiscountCode> findByCode(String code);

    /**
     * Acquires a pessimistic write lock before returning the discount code.
     * Serializes concurrent use-count increments to prevent two simultaneous
     * bookings from both reading the same currentUses value, both passing
     * maxUses validation, and both incrementing past the limit.
     *
     * Must be called within an active {@code @Transactional} context.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DiscountCode d WHERE d.code = :code")
    Optional<DiscountCode> findByCodeForUpdate(@Param("code") String code);

    List<DiscountCode> findAllByOrderByCreatedAtDesc();
}
