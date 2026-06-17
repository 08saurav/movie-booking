package com.example.booking.repository;

import com.example.booking.domain.PricingTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PricingTierRepository extends JpaRepository<PricingTier, Long> {

    List<PricingTier> findByNameContainingIgnoreCase(String name);
}
