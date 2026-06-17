package com.example.booking.web;

import com.example.booking.domain.DiscountCode;
import com.example.booking.domain.PricingTier;
import com.example.booking.repository.DiscountCodeRepository;
import com.example.booking.repository.PricingTierRepository;
import com.example.booking.service.BookingService;
import com.example.booking.web.dto.BookingResponse;
import com.example.booking.web.dto.DiscountCodeRequest;
import com.example.booking.web.dto.DiscountCodeResponse;
import com.example.booking.web.dto.PricingTierRequest;
import com.example.booking.web.dto.PricingTierResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin endpoints for pricing tiers, discount codes, and booking oversight.
 * All require ROLE_ADMIN (enforced by SecurityConfig pattern /api/admin/**).
 *
 * Segment 4: Pricing, Discounts, Payment & Confirmation.
 */
@RestController
@Tag(name = "Admin: Pricing & Discounts", description = "Manage pricing tiers, discount codes, and view all bookings (ROLE_ADMIN)")
public class PricingAdminController {

    private final PricingTierRepository pricingTierRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final BookingService bookingService;

    public PricingAdminController(PricingTierRepository pricingTierRepository,
                                   DiscountCodeRepository discountCodeRepository,
                                   BookingService bookingService) {
        this.pricingTierRepository = pricingTierRepository;
        this.discountCodeRepository = discountCodeRepository;
        this.bookingService = bookingService;
    }

    @PostMapping("/api/admin/pricing-tiers")
    @Operation(summary = "Create a pricing tier (ROLE_ADMIN)",
            description = "weekendMultiplier must be >= 1.00. After creation, assign to shows via the show creation/update endpoint.")
    public ResponseEntity<PricingTierResponse> createPricingTier(@Valid @RequestBody PricingTierRequest request) {
        PricingTier tier = new PricingTier(request.name(), request.regularPrice(),
                request.premiumPrice(), request.weekendMultiplier());
        return ResponseEntity.status(HttpStatus.CREATED).body(PricingTierResponse.from(pricingTierRepository.save(tier)));
    }

    @GetMapping("/api/admin/pricing-tiers")
    @Operation(summary = "List all pricing tiers (ROLE_ADMIN)")
    public List<PricingTierResponse> listPricingTiers() {
        return pricingTierRepository.findAll().stream().map(PricingTierResponse::from).toList();
    }

    @PostMapping("/api/admin/discount-codes")
    @Operation(summary = "Create a discount code (ROLE_ADMIN)",
            description = "discountType: PERCENTAGE (0–100) or FLAT (absolute amount). maxUses null = unlimited. "
                    + "validFrom/validTo null = no date restriction.")
    public ResponseEntity<DiscountCodeResponse> createDiscountCode(@Valid @RequestBody DiscountCodeRequest request) {
        DiscountCode code = new DiscountCode(request.code(), request.discountType(), request.value(),
                request.maxUses(), request.validFrom(), request.validTo(), request.active());
        return ResponseEntity.status(HttpStatus.CREATED).body(DiscountCodeResponse.from(discountCodeRepository.save(code)));
    }

    @GetMapping("/api/admin/discount-codes")
    @Operation(summary = "List all discount codes (ROLE_ADMIN)")
    public List<DiscountCodeResponse> listDiscountCodes() {
        return discountCodeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(DiscountCodeResponse::from).toList();
    }

    @GetMapping("/api/admin/bookings")
    @Operation(summary = "List all bookings with pagination (ROLE_ADMIN)",
            description = "Supports ?page=0&size=20&sort=createdAt,desc style pagination parameters.")
    public Page<BookingResponse> listBookings(@PageableDefault(size = 20) Pageable pageable) {
        return bookingService.listAll(pageable);
    }
}
