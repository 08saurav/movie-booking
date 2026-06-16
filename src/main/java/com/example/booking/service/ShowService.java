package com.example.booking.service;

import com.example.booking.domain.Movie;
import com.example.booking.domain.Screen;
import com.example.booking.domain.Seat;
import com.example.booking.domain.Show;
import com.example.booking.domain.ShowSeat;
import com.example.booking.domain.ShowSeatStatus;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.MovieRepository;
import com.example.booking.repository.ScreenRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.ShowRepository;
import com.example.booking.repository.ShowSeatRepository;
import com.example.booking.repository.TheaterRepository;
import com.example.booking.web.dto.ShowRequest;
import com.example.booking.web.dto.ShowRescheduleRequest;
import com.example.booking.web.dto.ShowResponse;
import com.example.booking.web.dto.ShowSeatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
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

    public ShowService(ShowRepository showRepository, MovieRepository movieRepository,
                        ScreenRepository screenRepository, SeatRepository seatRepository,
                        ShowSeatRepository showSeatRepository, TheaterRepository theaterRepository) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.showSeatRepository = showSeatRepository;
        this.theaterRepository = theaterRepository;
    }

    public ShowResponse create(ShowRequest request) {
        Movie movie = findMovieOrThrow(request.movieId());
        Screen screen = findScreenOrThrow(request.screenId());
        Instant endTime = request.startTime().plus(Duration.ofMinutes(movie.getDurationMinutes()));

        Show show = showRepository.save(new Show(movie, screen, request.startTime(), endTime));

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

    @Transactional(readOnly = true)
    public List<ShowResponse> listAll() {
        return showRepository.findAll().stream().map(ShowResponse::from).toList();
    }

    /** Upcoming shows for a theater (browse). 404s if the theater itself doesn't exist. */
    @Transactional(readOnly = true)
    public List<ShowResponse> listUpcomingForTheater(Long theaterId) {
        if (!theaterRepository.existsById(theaterId)) {
            throw new ResourceNotFoundException("Theater " + theaterId + " not found");
        }
        return showRepository.findByScreen_Theater_IdAndStartTimeAfterOrderByStartTimeAsc(theaterId, Instant.now())
                .stream()
                .map(ShowResponse::from)
                .toList();
    }

    /** Available-only seat inventory for a show (browse). 404s if the show doesn't exist. */
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> getAvailableSeats(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new ResourceNotFoundException("Show " + showId + " not found");
        }
        return showSeatRepository.findByShowIdAndStatus(showId, ShowSeatStatus.AVAILABLE).stream()
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
