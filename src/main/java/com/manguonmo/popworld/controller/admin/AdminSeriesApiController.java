package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.request.SeriesCreateRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.SeriesResponse;
import com.manguonmo.popworld.entity.Series;
import com.manguonmo.popworld.service.SeriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/api/series")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSeriesApiController {

    private final SeriesService seriesService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Series>>> getAllSeries() {
        return ResponseEntity.ok(ApiResponse.success(seriesService.getAllSeries()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SeriesResponse>> createSeries(@Valid @RequestBody SeriesCreateRequest request) {
        log.info("Admin đang tạo mới Series: name='{}', ipId={}", request.getName(), request.getCharacterIpId());
        SeriesResponse response = seriesService.createSeries(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo bộ sưu tập (Series) thành công!", response));
    }
}
