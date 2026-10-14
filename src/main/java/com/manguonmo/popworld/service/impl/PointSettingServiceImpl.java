package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.PointSettingUpdateRequest;
import com.manguonmo.popworld.entity.PointSetting;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.repository.PointSettingRepository;
import com.manguonmo.popworld.service.PointSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointSettingServiceImpl implements PointSettingService {

    private final PointSettingRepository pointSettingRepository;

    @Override
    @Transactional
    public PointSetting getSetting() {
        return pointSettingRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    log.info("Chưa có cấu hình điểm thưởng trong CSDL, tự động khởi tạo bản ghi mặc định...");
                    PointSetting defaultSetting = PointSetting.builder()
                            .earnRate(10000)
                            .redeemRate(100)
                            .maxRedeemPercent(20)
                            .enabled(true)
                            .build();
                    return pointSettingRepository.save(defaultSetting);
                });
    }

    @Override
    @Transactional
    public PointSetting updateSetting(PointSettingUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu cấu hình không hợp lệ");
        }

        if (request.getEarnRate() == null || request.getEarnRate() < 1000) {
            throw new BadRequestException("Tỷ lệ tích điểm phải từ 1.000 VNĐ trở lên");
        }

        if (request.getRedeemRate() == null || request.getRedeemRate() < 1 || request.getRedeemRate() > 10000) {
            throw new BadRequestException("Giá trị quy đổi 1 điểm phải từ 1 VNĐ đến 10.000 VNĐ");
        }

        if (request.getMaxRedeemPercent() == null || request.getMaxRedeemPercent() < 0 || request.getMaxRedeemPercent() > 100) {
            throw new BadRequestException("Phần trăm giảm tối đa phải từ 0% đến 100%");
        }

        if (request.getEarnRate() < request.getRedeemRate()) {
            throw new BadRequestException("Tỷ lệ tích điểm (" + request.getEarnRate() + " đ/điểm) không được nhỏ hơn tỷ lệ quy đổi (" + request.getRedeemRate() + " đ/điểm) để đảm bảo an toàn tài chính!");
        }

        PointSetting setting = getSetting();
        setting.setEarnRate(request.getEarnRate());
        setting.setRedeemRate(request.getRedeemRate());
        setting.setMaxRedeemPercent(request.getMaxRedeemPercent());
        setting.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        PointSetting saved = pointSettingRepository.save(setting);
        log.info("Admin đã cập nhật cấu hình điểm thưởng: earnRate={}, redeemRate={}, maxRedeemPercent={}%, enabled={}",
                saved.getEarnRate(), saved.getRedeemRate(), saved.getMaxRedeemPercent(), saved.getEnabled());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public int calculatePointsEarned(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        PointSetting setting = getSetting();
        if (!Boolean.TRUE.equals(setting.getEnabled()) || setting.getEarnRate() == null || setting.getEarnRate() <= 0) {
            return 0;
        }
        return orderAmount.divideToIntegralValue(BigDecimal.valueOf(setting.getEarnRate())).intValue();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculatePointsDiscount(int pointsToUse) {
        if (pointsToUse <= 0) {
            return BigDecimal.ZERO;
        }
        PointSetting setting = getSetting();
        if (!Boolean.TRUE.equals(setting.getEnabled()) || setting.getRedeemRate() == null || setting.getRedeemRate() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf((long) pointsToUse * setting.getRedeemRate());
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateMaxPointsUsable(BigDecimal subtotal, int userPoints) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0 || userPoints <= 0) {
            return 0;
        }
        PointSetting setting = getSetting();
        if (!Boolean.TRUE.equals(setting.getEnabled()) || setting.getRedeemRate() == null || setting.getRedeemRate() <= 0) {
            return 0;
        }
        BigDecimal maxDiscount = subtotal.multiply(BigDecimal.valueOf(setting.getMaxRedeemPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.FLOOR);
        int maxPointsFromSubtotal = maxDiscount.divideToIntegralValue(BigDecimal.valueOf(setting.getRedeemRate())).intValue();
        return Math.max(0, Math.min(userPoints, maxPointsFromSubtotal));
    }
}
