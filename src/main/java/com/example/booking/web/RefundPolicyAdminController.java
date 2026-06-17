package com.example.booking.web;

import com.example.booking.domain.RefundPolicy;
import com.example.booking.domain.RefundTier;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.RefundPolicyRepository;
import com.example.booking.repository.spec.RefundPolicySpec;
import com.example.booking.web.dto.RefundPolicyRequest;
import com.example.booking.web.dto.RefundPolicyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin endpoints for refund policy management. All require ROLE_ADMIN.
 * Segment 5: Cancellation, Refunds & Notifications.
 */
@RestController
@RequestMapping("/api/admin/refund-policies")
@Tag(name = "Admin: Refund Policies", description = "Manage refund policies and their tier ladders (ROLE_ADMIN)")
public class RefundPolicyAdminController {

    private final RefundPolicyRepository refundPolicyRepository;

    public RefundPolicyAdminController(RefundPolicyRepository refundPolicyRepository) {
        this.refundPolicyRepository = refundPolicyRepository;
    }

    @PostMapping
    @Operation(summary = "Create a refund policy (ROLE_ADMIN)",
            description = "Tiers define refund % based on hours before showtime. "
                    + "Set defaultPolicy=true to use this policy for shows with no explicit policy. "
                    + "Example tiers: [{hoursBeforeShow:24,refundPercent:100},{hoursBeforeShow:12,refundPercent:50},{hoursBeforeShow:0,refundPercent:0}]")
    public ResponseEntity<RefundPolicyResponse> create(@Valid @RequestBody RefundPolicyRequest request) {
        List<RefundTier> tiers = request.tiers().stream()
                .map(t -> new RefundTier(t.hoursBeforeShow(), t.refundPercent()))
                .toList();
        RefundPolicy policy = refundPolicyRepository.save(
                new RefundPolicy(request.name(), request.defaultPolicy(), tiers));
        return ResponseEntity.status(HttpStatus.CREATED).body(RefundPolicyResponse.from(policy));
    }

    @GetMapping
    @Operation(summary = "List refund policies (ROLE_ADMIN)",
            description = "Optional filters: isDefault (true/false), name (partial). All combinable.")
    public List<RefundPolicyResponse> list(
            @RequestParam(required = false) Boolean isDefault,
            @RequestParam(required = false) String name) {
        Specification<RefundPolicy> spec = Specification
                .where(RefundPolicySpec.isDefault(isDefault))
                .and(RefundPolicySpec.nameLike(name));
        return refundPolicyRepository.findAll(spec, Sort.by("name")).stream()
                .map(RefundPolicyResponse::from).toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a refund policy (ROLE_ADMIN)",
            description = "Replaces the policy name, default flag, and all tiers. Existing tiers are fully replaced.")
    public RefundPolicyResponse update(@PathVariable Long id, @Valid @RequestBody RefundPolicyRequest request) {
        RefundPolicy policy = refundPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RefundPolicy " + id + " not found"));
        List<RefundTier> tiers = request.tiers().stream()
                .map(t -> new RefundTier(t.hoursBeforeShow(), t.refundPercent()))
                .toList();
        policy.updateName(request.name());
        policy.setDefault(request.defaultPolicy());
        policy.updateTiers(tiers);
        return RefundPolicyResponse.from(refundPolicyRepository.save(policy));
    }
}
