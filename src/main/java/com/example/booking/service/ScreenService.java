package com.example.booking.service;

import com.example.booking.domain.Screen;
import com.example.booking.domain.Seat;
import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.Theater;
import com.example.booking.exception.InvalidRequestException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ScreenRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.TheaterRepository;
import com.example.booking.web.dto.ScreenRenameRequest;
import com.example.booking.web.dto.ScreenRequest;
import com.example.booking.web.dto.ScreenResponse;
import com.example.booking.web.dto.SeatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Owns seat-layout generation. A screen's seats are created exactly once, in
 * bulk, when the screen is created -- there is no separate "create a seat"
 * endpoint, by design (see the assignment brief: layout is defined at screen
 * creation time).
 */
@Service
@Transactional
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheaterRepository theaterRepository;
    private final SeatRepository seatRepository;

    public ScreenService(ScreenRepository screenRepository, TheaterRepository theaterRepository,
                          SeatRepository seatRepository) {
        this.screenRepository = screenRepository;
        this.theaterRepository = theaterRepository;
        this.seatRepository = seatRepository;
    }

    public ScreenResponse create(ScreenRequest request) {
        Theater theater = findTheaterOrThrow(request.theaterId());
        Set<String> rowLabels = rowLabelsFor(request.totalRows());
        Set<String> premiumRows = request.premiumRows() == null ? Set.of() : request.premiumRows();
        validatePremiumRows(premiumRows, rowLabels, request.totalRows());

        Screen screen = screenRepository.save(
                new Screen(theater, request.name(), request.totalRows(), request.totalCols()));

        List<Seat> seats = new ArrayList<>();
        for (int r = 1; r <= request.totalRows(); r++) {
            String rowLabel = rowLabel(r);
            SeatCategory category = premiumRows.contains(rowLabel) ? SeatCategory.PREMIUM : SeatCategory.REGULAR;
            for (int c = 1; c <= request.totalCols(); c++) {
                seats.add(new Seat(screen, rowLabel, c, category));
            }
        }
        seatRepository.saveAll(seats);

        return ScreenResponse.from(screen);
    }

    public ScreenResponse rename(Long id, ScreenRenameRequest request) {
        Screen screen = findScreenOrThrow(id);
        screen.setName(request.name());
        return ScreenResponse.from(screen);
    }

    public void delete(Long id) {
        screenRepository.delete(findScreenOrThrow(id));
    }

    @Transactional(readOnly = true)
    public ScreenResponse getById(Long id) {
        return ScreenResponse.from(findScreenOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> listAll(Long theaterId) {
        List<Screen> screens = theaterId != null
                ? screenRepository.findByTheaterId(theaterId)
                : screenRepository.findAll();
        return screens.stream().map(ScreenResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats(Long screenId) {
        findScreenOrThrow(screenId);
        return seatRepository.findByScreenIdOrderByRowLabelAscSeatNumberAsc(screenId).stream()
                .map(SeatResponse::from)
                .toList();
    }

    private void validatePremiumRows(Set<String> premiumRows, Set<String> validRowLabels, int totalRows) {
        for (String row : premiumRows) {
            if (!validRowLabels.contains(row)) {
                throw new InvalidRequestException(
                        "premiumRows contains '" + row + "', which is not a valid row label for a "
                                + totalRows + "-row screen (valid labels: " + String.join(", ", validRowLabels) + ")");
            }
        }
    }

    private Set<String> rowLabelsFor(int totalRows) {
        return IntStream.rangeClosed(1, totalRows).mapToObj(this::rowLabel).collect(Collectors.toSet());
    }

    private String rowLabel(int rowIndex1Based) {
        return String.valueOf((char) ('A' + rowIndex1Based - 1));
    }

    private Screen findScreenOrThrow(Long id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen " + id + " not found"));
    }

    private Theater findTheaterOrThrow(Long id) {
        return theaterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater " + id + " not found"));
    }
}
