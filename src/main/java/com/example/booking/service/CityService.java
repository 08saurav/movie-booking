package com.example.booking.service;

import com.example.booking.domain.City;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.CityRepository;
import com.example.booking.web.dto.CityRequest;
import com.example.booking.web.dto.CityResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public CityResponse create(CityRequest request) {
        City city = new City(request.name(), request.state(), request.country());
        return CityResponse.from(cityRepository.save(city));
    }

    public CityResponse update(Long id, CityRequest request) {
        City city = findCityOrThrow(id);
        city.setName(request.name());
        city.setState(request.state());
        city.setCountry(request.country());
        return CityResponse.from(city);
    }

    public void delete(Long id) {
        City city = findCityOrThrow(id);
        cityRepository.delete(city);
    }

    @Transactional(readOnly = true)
    public CityResponse getById(Long id) {
        return CityResponse.from(findCityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CityResponse> listAll() {
        return cityRepository.findAll().stream().map(CityResponse::from).toList();
    }

    private City findCityOrThrow(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City " + id + " not found"));
    }
}
