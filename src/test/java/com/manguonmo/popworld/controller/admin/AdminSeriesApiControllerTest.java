package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.request.SeriesCreateRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.SeriesResponse;
import com.manguonmo.popworld.entity.Series;
import com.manguonmo.popworld.service.SeriesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSeriesApiControllerTest {

    @Mock
    private SeriesService seriesService;

    @InjectMocks
    private AdminSeriesApiController controller;

    @Test
    @DisplayName("getAllSeries trả về danh sách Series")
    void getAllSeries_shouldReturnList() {
        Series s1 = Series.builder().id(1L).name("Series 1").build();
        when(seriesService.getAllSeries()).thenReturn(List.of(s1));

        ResponseEntity<ApiResponse<List<Series>>> response = controller.getAllSeries();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
        verify(seriesService, times(1)).getAllSeries();
    }

    @Test
    @DisplayName("createSeries trả về SeriesResponse khi tạo thành công")
    void createSeries_shouldReturnCreated() {
        SeriesCreateRequest request = SeriesCreateRequest.builder()
                .name("Hirono Echoes")
                .characterIpId(1L)
                .releaseDate(LocalDate.now())
                .build();

        SeriesResponse serviceResponse = SeriesResponse.builder()
                .id(100L)
                .name("Hirono Echoes")
                .characterIpId(1L)
                .characterIpName("Hirono")
                .build();

        when(seriesService.createSeries(any(SeriesCreateRequest.class))).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<SeriesResponse>> response = controller.createSeries(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(100L, response.getBody().getData().getId());
        assertEquals("Hirono Echoes", response.getBody().getData().getName());
        verify(seriesService, times(1)).createSeries(any(SeriesCreateRequest.class));
    }
}
