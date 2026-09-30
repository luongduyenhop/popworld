package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.PopNowSeriesTheme;
import com.manguonmo.popworld.entity.Product;

import java.util.Map;

/**
 * Service quản lý giao diện khay 3D POP NOW riêng biệt cho từng dòng sản phẩm (Series)
 */
public interface PopNowThemeService {

    PopNowSeriesTheme getThemeForProduct(Product product);

    PopNowSeriesTheme getThemeBySlug(String slug);

    Map<String, PopNowSeriesTheme> getAllThemes();
}
