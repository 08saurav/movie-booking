package com.example.booking.service;

import com.example.booking.domain.City;
import com.example.booking.domain.Theater;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.CityRepository;
import com.example.booking.repository.TheaterRepository;
import com.example.booking.repository.spec.TheaterSpec;
import com.example.booking.web.dto.TheaterRequest;
import com.example.booking.web.dto.TheaterResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TheaterService {

    private final TheaterRepository theaterRepository;
    private final CityRepository cityRepository;

    public TheaterService(TheaterRepository theaterRepository, CityRepository cityRepository) {
        this.theaterRepository = theaterRepository;
        this.cityRepository = cityRepository;
    }

    public TheaterResponse create(TheaterRequest request) {
        City city = findCityOrThrow(request.cityId());
        Theater theater = new Theater(city, request.name(), request.address());
        return TheaterResponse.from(theaterRepository.save(theater));
    }

    public TheaterResponse update(Long id, TheaterRequest request) {
        Theater theater = findTheaterOrThrow(id);
        theater.setCity(findCityOrThrow(request.cityId()));
        theater.setName(request.name());
        theater.setAddress(request.address());
        return TheaterResponse.from(theater);
    }

    public void delete(Long id) {
        theaterRepository.delete(findTheaterOrThrow(id));
    }

    @Transactional(readOnly = true)
    public TheaterResponse getById(Long id) {
        return TheaterResponse.from(findTheaterOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<TheaterResponse> listAll(Long cityId, String name) {
        Specification<Theater> spec = Specification
                .where(TheaterSpec.cityIdEq(cityId))
                .and(TheaterSpec.nameLike(name));
        return theaterRepository.findAll(spec, Sort.by("name")).stream().map(TheaterResponse::from).toList();
    }

    /** Used by the customer browse endpoint; 404s if the city itself doesn't exist. */
    @Transactional(readOnly = true)
    public List<TheaterResponse> listByCity(Long cityId, String name) {
        findCityOrThrow(cityId);
        Specification<Theater> spec = Specification
                .where(TheaterSpec.cityIdEq(cityId))
                .and(TheaterSpec.nameLike(name));
        return theaterRepository.findAll(spec, Sort.by("name")).stream().map(TheaterResponse::from).toList();
    }

    private Theater findTheaterOrThrow(Long id) {
        return theaterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater " + id + " not found"));
    }

    private City findCityOrThrow(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City " + id + " not found"));
    }
}
