package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.PointSettingUpdateRequest;
import com.manguonmo.popworld.entity.PointSetting;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.repository.PointSettingRepository;
import com.manguonmo.popworld.service.impl.PointSettingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointSettingServiceTest {

    @Mock
    private PointSettingRepository pointSettingRepository;

    @InjectMocks
    private PointSettingServiceImpl pointSettingService;

    private PointSetting defaultSetting;

    @BeforeEach
    void setUp() {
        defaultSetting = PointSetting.builder()
                .id(1L)
                .earnRate(10000)
                .redeemRate(100)
                .maxRedeemPercent(20)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("getSetting: Khi đã có cấu hình trong DB thì trả về cấu hình đó")
    void getSetting_WhenExists_ShouldReturnSetting() {
        when(pointSettingRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(defaultSetting));

        PointSetting result = pointSettingService.getSetting();

        assertThat(result).isNotNull();
        assertThat(result.getEarnRate()).isEqualTo(10000);
        assertThat(result.getRedeemRate()).isEqualTo(100);
        assertThat(result.getMaxRedeemPercent()).isEqualTo(20);
        assertThat(result.getEnabled()).isTrue();
        verify(pointSettingRepository, never()).save(any());
    }

    @Test
    @DisplayName("getSetting: Khi chưa có cấu hình trong DB thì tự động tạo cấu hình mặc định (Self-healing)")
    void getSetting_WhenNotExists_ShouldCreateDefault() {
        when(pointSettingRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(pointSettingRepository.save(any(PointSetting.class))).thenAnswer(invocation -> {
            PointSetting setting = invocation.getArgument(0);
            setting.setId(1L);
            return setting;
        });

        PointSetting result = pointSettingService.getSetting();

        assertThat(result).isNotNull();
        assertThat(result.getEarnRate()).isEqualTo(10000);
        assertThat(result.getRedeemRate()).isEqualTo(100);
        assertThat(result.getMaxRedeemPercent()).isEqualTo(20);
        assertThat(result.getEnabled()).isTrue();
        verify(pointSettingRepository, times(1)).save(any(PointSetting.class));
    }

    @Test
    @DisplayName("updateSetting: Cập nhật thành công khi dữ liệu hợp lệ")
    void updateSetting_ValidRequest_ShouldUpdateAndReturn() {
        when(pointSettingRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(defaultSetting));
        when(pointSettingRepository.save(any(PointSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PointSettingUpdateRequest request = PointSettingUpdateRequest.builder()
                .earnRate(5000)
                .redeemRate(200)
                .maxRedeemPercent(30)
                .enabled(true)
                .build();

        PointSetting updated = pointSettingService.updateSetting(request);

        assertThat(updated.getEarnRate()).isEqualTo(5000);
        assertThat(updated.getRedeemRate()).isEqualTo(200);
        assertThat(updated.getMaxRedeemPercent()).isEqualTo(30);
        assertThat(updated.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("updateSetting: Ném lỗi nếu earnRate < redeemRate (Chống chênh lệch ngược / Anti-arbitrage)")
    void updateSetting_EarnRateLessThanRedeemRate_ShouldThrowException() {
        PointSettingUpdateRequest request = PointSettingUpdateRequest.builder()
                .earnRate(500)
                .redeemRate(1000)
                .maxRedeemPercent(20)
                .enabled(true)
                .build();

        assertThatThrownBy(() -> pointSettingService.updateSetting(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("calculatePointsEarned: Tính điểm tích lũy chính xác")
    void calculatePointsEarned_ShouldCalculateCorrectly() {
        when(pointSettingRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(defaultSetting));

        // 250.000 / 10.000 = 25 points
        int points = pointSettingService.calculatePointsEarned(BigDecimal.valueOf(250000));
        assertThat(points).isEqualTo(25);

        // 9.999 / 10.000 = 0 points
        int zeroPoints = pointSettingService.calculatePointsEarned(BigDecimal.valueOf(9999));
        assertThat(zeroPoints).isEqualTo(0);

        // null hoặc <= 0
        assertThat(pointSettingService.calculatePointsEarned(null)).isEqualTo(0);
        assertThat(pointSettingService.calculatePointsEarned(BigDecimal.ZERO)).isEqualTo(0);
    }

    @Test
    @DisplayName("calculatePointsEarned: Trả về 0 nếu hệ thống điểm bị tắt")
    void calculatePointsEarned_WhenDisabled_ShouldReturnZero() {
        defaultSetting.setEnabled(false);
        when(pointSettingRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(defaultSetting));

        int points = pointSettingService.calculatePointsEarned(BigDecimal.valueOf(500000));
        assertThat(points).isEqualTo(0);
    }

    @Test
    @DisplayName("calculatePointsDiscount: Tính số tiền giảm giá chính xác")
    void calculatePointsDiscount_ShouldCalculateCorrectly() {
        when(pointSettingRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(defaultSetting));

        // 50 points * 100 VND = 5.000 VND
        BigDecimal discount = pointSettingService.calculatePointsDiscount(50);
        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(5000));

        // <= 0 points
        assertThat(pointSettingService.calculatePointsDiscount(0)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateMaxPointsUsable: Giới hạn theo số điểm của khách và % tối đa của đơn")
    void calculateMaxPointsUsable_ShouldRespectCaps() {
        when(pointSettingRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(defaultSetting));

        // Đơn 500.000 đ. Max 20% = 100.000 đ. 1 point = 100 đ => Max 1.000 points.
        // Khách có 200 points -> được dùng tối đa 200 points.
        int usable1 = pointSettingService.calculateMaxPointsUsable(BigDecimal.valueOf(500000), 200);
        assertThat(usable1).isEqualTo(200);

        // Khách có 1500 points -> bị giới hạn ở 1.000 points.
        int usable2 = pointSettingService.calculateMaxPointsUsable(BigDecimal.valueOf(500000), 1500);
        assertThat(usable2).isEqualTo(1000);
    }
}
