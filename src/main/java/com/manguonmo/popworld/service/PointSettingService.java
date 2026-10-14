package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.PointSettingUpdateRequest;
import com.manguonmo.popworld.entity.PointSetting;

import java.math.BigDecimal;

public interface PointSettingService {

    PointSetting getSetting();

    PointSetting updateSetting(PointSettingUpdateRequest request);

    int calculatePointsEarned(BigDecimal orderAmount);

    BigDecimal calculatePointsDiscount(int pointsToUse);

    int calculateMaxPointsUsable(BigDecimal subtotal, int userPoints);
}
