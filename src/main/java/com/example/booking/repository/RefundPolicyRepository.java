package com.example.booking.repository;

import com.example.booking.domain.RefundPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long>, JpaSpecificationExecutor<RefundPolicy> {

    Optional<RefundPolicy> findByDefaultPolicyTrue();
}
