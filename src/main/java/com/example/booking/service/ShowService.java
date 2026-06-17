package com.example.booking.service;

import com.example.booking.domain.Movie;
import com.example.booking.domain.PricingTier;
import com.example.booking.domain.RefundPolicy;
import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.Screen;
import com.example.booking.domain.Seat;
import com.example.booking.domain.Show;
import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.MovieRepository;
import com.example.booking.repository.PricingTierRepository;
import com.example.booking.repository.RefundPolicyRepository;
import com.example.booking.repository.ScreenRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.ShowRepository;
import com.example.booking.repository.ShowSeatRepository;
import com.example.booking.repository.TheaterRepository;
import com.example.booking.repository.spec.ShowSeatSpec;
import com.example.booking.repository.spec.ShowSpec;
import com.example.booking.web.dto.ShowRequest;
import com.example.booking.web.dto.ShowRescheduleRequest;
import com.example.booking.web.dto.ShowResponse;
import com.example.booking.web.dto.ShowSeatResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Owns per-show seat inventory generation: creating a show materializes one
 * {@link ShowSeat} row per {@link Seat} on its screen, all AVAILABLE. This
 * mirrors {@code ScreenService} generating seats from a screen's dimensions.
 */
@Service
@Transactional
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final TheaterRepository theaterRepository;
    private final PricingTierRepository pricingTierRepository;
    private final RefundPolicyRepository refundPolicyRepository;

    public ShowService(ShowRepository showRepository, MovieRepository movieRepository,
                        ScreenRepository screenRepository, SeatRepository seatRepository,
                        ShowSeatRepository showSeatRepository, TheaterRepository theaterRepository,
                        PricingTierRepository pricingTierRepository,
                        RefundPolicyRepository refundPolicyRepository) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.showSeatRepository = showSeatRepository;
        this.theaterRepository = theaterRepository;
        this.pricingTierRepository = pricingTierRepository;
        this.refundPolicyRepository = refundPolicyRepository;
    }

    public ShowResponse create(ShowRequest request) {
        Movie movie = findMovieOrThrow(request.movieId());
        Screen screen = findScreenOrThrow(request.screenId());
        Instant endTime = request.startTime().plus(Duration.ofMinutes(movie.getDurationMinutes()));

        PricingTier pricingTier = null;
        if (request.pricingTierId() != null) {
            pricingTier = pricingTierRepository.findById(request.pricingTierId())
                    .orElseThrow(() -> new ResourceNotFoundException("PricingTier " + request.pricingTierId() + " not found"));
        }

        RefundPolicy refundPolicy = null;
        if (request.refundPolicyId() != null) {
            refundPolicy = refundPolicyRepository.findById(request.refundPolicyId())
                    .orElseThrow(() -> new ResourceNotFoundException("RefundPolicy " + request.refundPolicyId() + " not found"));
        }

        Show show = showRepository.save(new Show(movie, screen, request.startTime(), endTime, pricingTier, refundPolicy));

        List<Seat> seats = seatRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(screen.getId());
        List<ShowSeat> showSeats = seats.stream()
                .map(seat -> new ShowSeat(show, seat, ShowSeatStatus.AVAILABLE))
                .toList();
        showSeatRepository.saveAll(showSeats);

        return ShowResponse.from(show);
    }

    public ShowResponse reschedule(Long id, ShowRescheduleRequest request) {
        Show show = findShowOrThrow(id);
        Instant endTime = request.startTime().plus(Duration.ofMinutes(show.getMovie().getDurationMinutes()));
        show.reschedule(request.startTime(), endTime);
        return ShowResponse.from(show);
    }

    public void delete(Long id) {
        showRepository.delete(findShowOrThrow(id));
    }

    @Transactional(readOnly = true)
    public ShowResponse getById(Long id) {
        return ShowResponse.from(findShowOrThrow(id));
    }

    /** Admin: list all shows with optional filters. */
    @Transactional(readOnly = true)
    public List<ShowResponse> listAll(Long movieId, Long screenId, Long theaterId, Long cityId,
                                      LocalDate date, LocalDate from, LocalDate to,
                                      String language, String genre) {
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Specification<Show> spec = Specification
                .where(ShowSpec.movieIdEq(movieId))
                .and(ShowSpec.screenIdEq(screenId))
                .and(ShowSpec.theaterIdEq(theaterId))
                .and(ShowSpec.cityIdEq(cityId))
                .and(ShowSpec.onDate(date))
                .and(ShowSpec.startFrom(fromInstant))
                .and(ShowSpec.startBefore(toInstant))
                .and(ShowSpec.languageEq(language))
                .and(ShowSpec.genreEq(genre));
        return showRepository.findAll(spec, Sort.by("startTime").ascending()).stream()
                .map(ShowResponse::from)
                .toList();
    }

    /**
     * Customer browse: upcoming shows for a theater with optional filters.
     * 404s if the theater itself doesn't exist.
     */
    @Transactional(readOnly = true)
    public List<ShowResponse> listUpcomingForTheater(Long theaterId, Long movieId, String movieTitle,
                                                     LocalDate date, String language, String genre) {
        if (!theaterRepository.existsById(theaterId)) {
            throw new ResourceNotFoundException("Theater " + theaterId + " not found");
        }
        Specification<Show> spec = Specification
                .where(ShowSpec.theaterIdEq(theaterId))
                .and(ShowSpec.startAfter(Instant.now()))
                .and(ShowSpec.movieIdEq(movieId))
                .and(ShowSpec.movieTitleLike(movieTitle))
                .and(ShowSpec.onDate(date))
                .and(ShowSpec.languageEq(language))
                .and(ShowSpec.genreEq(genre));
        return showRepository.findAll(spec, Sort.by("startTime").ascending()).stream()
                .map(ShowResponse::from)
                .toList();
    }

    /**
     * Seat inventory for a show with real-time status and optional filters.
     * 404s if the show doesn't exist.
     */
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> getSeatsWithStatus(Long showId, ShowSeatStatus status,
                                                     SeatCategory category, String rowLabel) {
        if (!showRepository.existsById(showId)) {
            throw new ResourceNotFoundException("Show " + showId + " not found");
        }
        Specification<ShowSeat> spec = Specification
                .where(ShowSeatSpec.showIdEq(showId))
                .and(ShowSeatSpec.statusEq(status))
                .and(ShowSeatSpec.categoryEq(category))
                .and(ShowSeatSpec.rowLabelEq(rowLabel));
        return showSeatRepository.findAll(spec, Sort.by("seat.rowLabel", "seat.seatNumber")).stream()
                .map(ShowSeatResponse::from)
                .toList();
    }

    private Show findShowOrThrow(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show " + id + " not found"));
    }

    private Movie findMovieOrThrow(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie " + id + " not found"));
    }

    private Screen findScreenOrThrow(Long id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen " + id + " not found"));
    }
}
