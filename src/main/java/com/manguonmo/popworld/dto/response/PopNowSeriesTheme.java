package com.manguonmo.popworld.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Cấu hình Theme giao diện khay 3D POP NOW cho từng Series/Sản phẩm
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopNowSeriesTheme implements Serializable {

    private String seriesKey;
    private String seriesName;
    private String trayBgUrl;
    private String smallBoxImgUrl;
    private String trayFrontUrl;
    private String trayShadowUrl;
    private String guideHandUrl;
    private Integer totalSlots;
    private String primaryColor;
    private String accentColor;
    private String backgroundColor;
}
