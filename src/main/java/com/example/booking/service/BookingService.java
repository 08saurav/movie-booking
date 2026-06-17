package com.example.booking.service;

import com.example.booking.domain.Booking;
import com.example.booking.domain.BookingStatus;
import com.example.booking.domain.DiscountCode;
import com.example.booking.domain.PricingTier;
import com.example.booking.domain.RefundPolicy;
import com.example.booking.domain.RefundTier;
import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.Show;
import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import com.example.booking.event.BookingCancelledEvent;
import com.example.booking.event.BookingConfirmedEvent;
import com.example.booking.exception.InvalidRequestException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.exception.SeatNotAvailableException;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.DiscountCodeRepository;
import com.example.booking.repository.RefundPolicyRepository;
import com.example.booking.repository.ShowRepository;
import com.example.booking.repository.ShowSeatRepository;
import com.example.booking.repository.spec.BookingSpec;
import com.example.booking.web.dto.BookingRequest;
import com.example.booking.web.dto.BookingResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates the booking flow: validate hold → validate discount (with
 * pessimistic lock) → calculate price → create PAYMENT_PENDING record →
 * atomically transition seat HELD→BOOKED → run mock payment → CONFIRMED or
 * PAYMENT_FAILED (seat released to AVAILABLE).
 *
 * Idempotent: submitting the same X-Idempotency-Key twice returns the
 * existing booking without re-processing.
 */
@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final RefundPolicyRepository refundPolicyRepository;
    private final PricingService pricingService;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(BookingRepository bookingRepository,
                          ShowRepository showRepository,
                          ShowSeatRepository showSeatRepository,
                          DiscountCodeRepository discountCodeRepository,
                          RefundPolicyRepository refundPolicyRepository,
                          PricingService pricingService,
                          PaymentGateway paymentGateway,
                          ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.discountCodeRepository = discountCodeRepository;
        this.refundPolicyRepository = refundPolicyRepository;
        this.pricingService = pricingService;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }

    public BookingResponse book(String customer, BookingRequest request, String idempotencyKey) {
        return bookingRepository.findByIdempotencyKey(idempotencyKey)
                .map(BookingResponse::from)
                .orElseGet(() -> processNewBooking(customer, request, idempotencyKey));
    }

    private BookingResponse processNewBooking(String customer, BookingRequest request, String idempotencyKey) {
        Show show = showRepository.findById(request.showId())
                .orElseThrow(() -> new ResourceNotFoundException("Show " + request.showId() + " not found"));

        ShowSeat showSeat = showSeatRepository.findById(request.showSeatId())
                .orElseThrow(() -> new ResourceNotFoundException("ShowSeat " + request.showSeatId() + " not found"));

        if (!showSeat.getShow().getId().equals(show.getId())) {
            throw new InvalidRequestException("Seat " + request.showSeatId() + " does not belong to show " + request.showId());
        }
        if (showSeat.getStatus() != ShowSeatStatus.HELD) {
            throw new SeatNotAvailableException("Seat is not in HELD state — call the hold endpoint first");
        }
        if (!customer.equals(showSeat.getHeldBy())) {
            throw new SeatNotAvailableException("Seat is held by a different customer");
        }
        if (showSeat.getHoldExpiresAt().isBefore(Instant.now())) {
            throw new SeatNotAvailableException("Hold has expired — re-hold the seat and try again");
        }

        PricingTier pricingTier = show.getPricingTier();
        if (pricingTier == null) {
            throw new InvalidRequestException("Show " + show.getId() + " has no pricing tier — admin must assign one");
        }

        // Pessimistic lock on discount code row prevents concurrent over-use
        DiscountCode discountCode = null;
        if (request.discountCode() != null && !request.discountCode().isBlank()) {
            discountCode = discountCodeRepository.findByCodeForUpdate(request.discountCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Discount code not found: " + request.discountCode()));
            validateDiscountCode(discountCode);
            discountCode.incrementUses();
        }

        SeatCategory category = showSeat.getSeat().getCategory();
        PricingService.PriceBreakdown price = pricingService.calculate(pricingTier, category, show, discountCode);

        // Transition HELD→BOOKED before payment so the expiry sweeper cannot reclaim the seat
        // during payment processing. Only succeeds if the seat is still HELD by this customer.
        int booked = showSeatRepository.bookSeat(showSeat.getId(), customer);
        if (booked == 0) {
            throw new SeatNotAvailableException("Seat state changed concurrently — please try again");
        }

        Booking booking = bookingRepository.save(new Booking(
                customer, show, showSeat, pricingTier, discountCode,
                price.basePrice(), price.discountAmount(), price.finalPrice(), idempotencyKey));

        boolean paymentSuccess = paymentGateway.charge(customer, price.finalPrice(), "BOOKING-" + booking.getId());

        if (paymentSuccess) {
            booking.confirm();
            // Event queued here, fired after transaction commits (AFTER_COMMIT listener)
            eventPublisher.publishEvent(new BookingConfirmedEvent(
                    booking.getId(), customer,
                    show.getMovie().getTitle(),
                    show.getScreen().getTheater().getName(),
                    showSeat.getSeat().getRowLabel() + showSeat.getSeat().getSeatNumber(),
                    booking.getFinalPrice()));
        } else {
            booking.markPaymentFailed();
            // Seat returns to inventory so others can book it
            showSeatRepository.releaseBookedSeat(showSeat.getId());
        }

        return BookingResponse.from(booking);
    }

    /**
     * Cancel a booking. Customers can only cancel their own CONFIRMED bookings.
     * {@code isAdmin=true} allows cancelling any CONFIRMED booking regardless of owner.
     *
     * Flow: validate status → compute refund → mark CANCELLED → release seat →
     *   publish BookingCancelledEvent (fires after commit on async thread).
     */
    public BookingResponse cancel(Long bookingId, String requester, boolean isAdmin) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingId + " not found"));

        if (!isAdmin && !requester.equals(booking.getCustomer())) {
            throw new ResourceNotFoundException("Booking " + bookingId + " not found");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidRequestException(
                    "Only CONFIRMED bookings can be cancelled (current status: " + booking.getStatus() + ")");
        }

        BigDecimal refundAmount = computeRefund(booking);
        Show show = booking.getShow();
        ShowSeat showSeat = booking.getShowSeat();

        booking.cancel(refundAmount);
        showSeatRepository.releaseBookedSeat(showSeat.getId());

        eventPublisher.publishEvent(new BookingCancelledEvent(
                booking.getId(), booking.getCustomer(),
                show.getMovie().getTitle(),
                show.getScreen().getTheater().getName(),
                showSeat.getSeat().getRowLabel() + showSeat.getSeat().getSeatNumber(),
                refundAmount));

        return BookingResponse.from(booking);
    }

    /**
     * Determines the refund amount based on the show's refund policy and how far
     * in the future the show is. Falls back to the default policy if the show has
     * none; returns ZERO if no policy exists at all.
     *
     * Logic: find the policy tier whose {@code hoursBeforeShow} is the largest
     * value still ≤ hours remaining until the show. Sort candidates descending
     * and take the first match.
     */
    private BigDecimal computeRefund(Booking booking) {
        Show show = booking.getShow();
        RefundPolicy policy = show.getRefundPolicy();
        if (policy == null) {
            policy = refundPolicyRepository.findByDefaultPolicyTrue().orElse(null);
        }
        if (policy == null || policy.getTiers().isEmpty()) {
            return BigDecimal.ZERO;
        }

        long hoursUntilShow = ChronoUnit.HOURS.between(Instant.now(), show.getStartTime());
        if (hoursUntilShow < 0) {
            return BigDecimal.ZERO;
        }

        final long hours = hoursUntilShow;
        int refundPercent = policy.getTiers().stream()
                .filter(t -> t.getHoursBeforeShow() <= hours)
                .max(Comparator.comparingInt(RefundTier::getHoursBeforeShow))
                .map(RefundTier::getRefundPercent)
                .orElse(0);

        return booking.getFinalPrice()
                .multiply(BigDecimal.valueOf(refundPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listForCustomer(String customer, BookingStatus status,
                                                 LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("'from' date must not be after 'to' date");
        }
        Specification<Booking> spec = Specification
                .where(BookingSpec.customerEq(customer))
                .and(BookingSpec.statusEq(status))
                .and(BookingSpec.fromDate(from))
                .and(BookingSpec.toDate(to));
        return bookingRepository.findAll(spec, pageable).map(BookingResponse::from);
    }

    @Transactional(readOnly = true)
    public BookingResponse getForCustomer(Long id, String customer) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + id + " not found"));
        // Return not-found rather than 403 to avoid leaking existence of other customers' bookings
        if (!customer.equals(booking.getCustomer())) {
            throw new ResourceNotFoundException("Booking " + id + " not found");
        }
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(Long id) {
        return BookingResponse.from(bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + id + " not found")));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listAll(Pageable pageable, BookingStatus status, String customer,
                                         Long showId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("'from' date must not be after 'to' date");
        }
        Specification<Booking> spec = Specification
                .where(BookingSpec.statusEq(status))
                .and(BookingSpec.customerEq(customer))
                .and(BookingSpec.showIdEq(showId))
                .and(BookingSpec.fromDate(from))
                .and(BookingSpec.toDate(to));
        return bookingRepository.findAll(spec, pageable).map(BookingResponse::from);
    }

    private void validateDiscountCode(DiscountCode code) {
        if (!code.isActive()) {
            throw new InvalidRequestException("Discount code is inactive: " + code.getCode());
        }
        if (code.getMaxUses() != null && code.getCurrentUses() >= code.getMaxUses()) {
            throw new InvalidRequestException("Discount code has reached its maximum usage limit");
        }
        Instant now = Instant.now();
        if (code.getValidFrom() != null && now.isBefore(code.getValidFrom())) {
            throw new InvalidRequestException("Discount code is not yet valid");
        }
        if (code.getValidTo() != null && now.isAfter(code.getValidTo())) {
            throw new InvalidRequestException("Discount code has expired");
        }
    }
}
