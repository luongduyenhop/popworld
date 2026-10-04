package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.SeriesCreateRequest;
import com.manguonmo.popworld.dto.response.SeriesResponse;
import com.manguonmo.popworld.entity.CharacterIp;
import com.manguonmo.popworld.entity.Series;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.CharacterIpRepository;
import com.manguonmo.popworld.repository.SeriesRepository;
import com.manguonmo.popworld.service.impl.SeriesServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeriesServiceTest {

    @Mock
    private SeriesRepository seriesRepository;

    @Mock
    private CharacterIpRepository characterIpRepository;

    @InjectMocks
    private SeriesServiceImpl seriesService;

    @Test
    @DisplayName("getAllSeries trả về danh sách series sắp xếp theo ngày phát hành")
    void getAllSeries_shouldReturnList() {
        Series s1 = Series.builder().id(1L).name("Series 1").build();
        Series s2 = Series.builder().id(2L).name("Series 2").build();

        when(seriesRepository.findAllByOrderByReleaseDateDesc()).thenReturn(List.of(s1, s2));

        List<Series> result = seriesService.getAllSeries();

        assertEquals(2, result.size());
        assertEquals("Series 1", result.get(0).getName());
        verify(seriesRepository, times(1)).findAllByOrderByReleaseDateDesc();
    }

    @Test
    @DisplayName("createSeries thành công khi dữ liệu hợp lệ")
    void createSeries_success() {
        CharacterIp ip = CharacterIp.builder().id(10L).name("Hirono").build();
        SeriesCreateRequest request = SeriesCreateRequest.builder()
                .name("Little Mischief")
                .characterIpId(10L)
                .releaseDate(LocalDate.of(2025, 1, 1))
                .build();

        Series savedSeries = Series.builder()
                .id(1L)
                .name("Little Mischief")
                .characterIp(ip)
                .releaseDate(LocalDate.of(2025, 1, 1))
                .build();

        when(seriesRepository.findByName("Little Mischief")).thenReturn(Optional.empty());
        when(characterIpRepository.findById(10L)).thenReturn(Optional.of(ip));
        when(seriesRepository.save(any(Series.class))).thenReturn(savedSeries);

        SeriesResponse response = seriesService.createSeries(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Little Mischief", response.getName());
        assertEquals("Hirono", response.getCharacterIpName());
        verify(seriesRepository, times(1)).save(any(Series.class));
    }

    @Test
    @DisplayName("createSeries trả về series đã có nếu trùng tên")
    void createSeries_duplicateName_returnsExisting() {
        CharacterIp ip = CharacterIp.builder().id(10L).name("Hirono").build();
        Series existing = Series.builder()
                .id(99L)
                .name("Existing Series")
                .characterIp(ip)
                .build();

        SeriesCreateRequest request = SeriesCreateRequest.builder()
                .name("Existing Series")
                .characterIpId(10L)
                .build();

        when(seriesRepository.findByName("Existing Series")).thenReturn(Optional.of(existing));

        SeriesResponse response = seriesService.createSeries(request);

        assertNotNull(response);
        assertEquals(99L, response.getId());
        verify(seriesRepository, never()).save(any(Series.class));
    }

    @Test
    @DisplayName("createSeries ném lỗi khi thiếu tên hoặc nhân vật IP")
    void createSeries_validationFailures() {
        SeriesCreateRequest emptyName = SeriesCreateRequest.builder().name("   ").build();
        assertThrows(BadRequestException.class, () -> seriesService.createSeries(emptyName));

        SeriesCreateRequest noIp = SeriesCreateRequest.builder().name("Series Test").characterIpId(null).build();
        assertThrows(BadRequestException.class, () -> seriesService.createSeries(noIp));

        SeriesCreateRequest notFoundIp = SeriesCreateRequest.builder().name("Series Test").characterIpId(999L).build();
        when(seriesRepository.findByName("Series Test")).thenReturn(Optional.empty());
        when(characterIpRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> seriesService.createSeries(notFoundIp));
    }
}
