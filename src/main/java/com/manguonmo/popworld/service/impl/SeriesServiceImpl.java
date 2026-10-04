package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.SeriesCreateRequest;
import com.manguonmo.popworld.dto.response.SeriesResponse;
import com.manguonmo.popworld.entity.CharacterIp;
import com.manguonmo.popworld.entity.Series;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.CharacterIpRepository;
import com.manguonmo.popworld.repository.SeriesRepository;
import com.manguonmo.popworld.service.SeriesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeriesServiceImpl implements SeriesService {

    private final SeriesRepository seriesRepository;
    private final CharacterIpRepository characterIpRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Series> getAllSeries() {
        return seriesRepository.findAllByOrderByReleaseDateDesc();
    }

    @Override
    @Transactional
    public SeriesResponse createSeries(SeriesCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu tạo Series không hợp lệ!");
        }

        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw new BadRequestException("Tên Series không được để trống!");
        }

        // Kiểm tra trùng tên Series
        Optional<Series> existing = seriesRepository.findByName(name);
        if (existing.isPresent()) {
            Series s = existing.get();
            log.info("Series '{}' đã tồn tại với ID={}", name, s.getId());
            return mapToResponse(s);
        }

        if (request.getCharacterIpId() == null) {
            throw new BadRequestException("Vui lòng chọn Nhân vật / IP đại diện cho Series!");
        }

        CharacterIp characterIp = characterIpRepository.findById(request.getCharacterIpId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Nhân vật / IP với ID: " + request.getCharacterIpId()));

        Series series = Series.builder()
                .name(name)
                .characterIp(characterIp)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .bannerUrl(request.getBannerUrl() != null && !request.getBannerUrl().isBlank() ? request.getBannerUrl().trim() : null)
                .releaseDate(request.getReleaseDate() != null ? request.getReleaseDate() : LocalDate.now())
                .build();

        Series saved = seriesRepository.save(series);
        log.info("Admin đã tạo mới thành công Series id={}, name='{}', IP='{}'", saved.getId(), saved.getName(), characterIp.getName());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Series> findById(Long id) {
        return seriesRepository.findById(id);
    }

    private SeriesResponse mapToResponse(Series series) {
        return SeriesResponse.builder()
                .id(series.getId())
                .name(series.getName())
                .characterIpId(series.getCharacterIp() != null ? series.getCharacterIp().getId() : null)
                .characterIpName(series.getCharacterIp() != null ? series.getCharacterIp().getName() : "")
                .bannerUrl(series.getBannerUrl())
                .releaseDate(series.getReleaseDate())
                .build();
    }
}
