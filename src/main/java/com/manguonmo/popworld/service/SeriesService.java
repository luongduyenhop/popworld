package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.SeriesCreateRequest;
import com.manguonmo.popworld.dto.response.SeriesResponse;
import com.manguonmo.popworld.entity.Series;

import java.util.List;
import java.util.Optional;

public interface SeriesService {
    List<Series> getAllSeries();
    SeriesResponse createSeries(SeriesCreateRequest request);
    Optional<Series> findById(Long id);
}
