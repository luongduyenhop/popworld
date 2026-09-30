package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.response.PopNowSeriesTheme;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.service.PopNowThemeService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PopNowThemeServiceImpl implements PopNowThemeService {

    private final Map<String, PopNowSeriesTheme> themes = new LinkedHashMap<>();
    private final PopNowSeriesTheme defaultTheme;

    public PopNowThemeServiceImpl() {
        // 1. The Monsters (Labubu) - Khay Xanh Ngọc Teal
        PopNowSeriesTheme monsters = PopNowSeriesTheme.builder()
                .seriesKey("monsters")
                .seriesName("The Monsters (Labubu)")
                .trayBgUrl("https://prod-global-biz.popmart.com/globalAdmin/1781855858883_c107a9b26eee8ea1e35e47974d0e507e.png")
                .smallBoxImgUrl("https://prod-global-biz.popmart.com/globalAdmin/1781855858883_ec105be95fcad037caf69a67dfe02d35.png")
                .trayFrontUrl("https://prod-global-biz.popmart.com/globalAdmin/1781855858883_91bb21168bdcb0b9f113d63a88f49b0f.png")
                .trayShadowUrl("/images/popnow/tray-shadow.png")
                .guideHandUrl("/images/popnow/hand-guide.png")
                .totalSlots(6)
                .primaryColor("#008080")
                .accentColor("#F5C518")
                .backgroundColor("#F8FBFB")
                .build();
        themes.put("monsters", monsters);
        themes.put("labubu", monsters);

        // 2. Hirono - Khay Khói Tối & Hộp Hirono After Dark / Little Mischief
        PopNowSeriesTheme hirono = PopNowSeriesTheme.builder()
                .seriesKey("hirono")
                .seriesName("Hirono Series")
                .trayBgUrl("https://prod-global-biz.popmart.com/globalAdmin/1787904828277_c107a9b26eee8ea1e35e47974d0e507e.png")
                .smallBoxImgUrl("https://prod-global-biz.popmart.com/globalAdmin/1787904828277_ec105be95fcad037caf69a67dfe02d35.png")
                .trayFrontUrl("https://prod-global-biz.popmart.com/globalAdmin/1787904828277_91bb21168bdcb0b9f113d63a88f49b0f.png")
                .trayShadowUrl("/images/popnow/tray-shadow.png")
                .guideHandUrl("/images/popnow/hand-guide.png")
                .totalSlots(6)
                .primaryColor("#2C2D30")
                .accentColor("#E59866")
                .backgroundColor("#F5F5F7")
                .build();
        themes.put("hirono", hirono);

        // 3. Skullpanda - Khay Tím Dạ Quang & Hộp Skullpanda City of Night
        PopNowSeriesTheme skullpanda = PopNowSeriesTheme.builder()
                .seriesKey("skullpanda")
                .seriesName("Skullpanda Series")
                .trayBgUrl("https://prod-global-biz.popmart.com/globalAdmin/1789802239696_c107a9b26eee8ea1e35e47974d0e507e.png")
                .smallBoxImgUrl("https://prod-global-biz.popmart.com/globalAdmin/1789802239696_ec105be95fcad037caf69a67dfe02d35.png")
                .trayFrontUrl("https://prod-global-biz.popmart.com/globalAdmin/1789802239696_91bb21168bdcb0b9f113d63a88f49b0f.png")
                .trayShadowUrl("/images/popnow/tray-shadow.png")
                .guideHandUrl("/images/popnow/hand-guide.png")
                .totalSlots(6)
                .primaryColor("#3B1C52")
                .accentColor("#D980FA")
                .backgroundColor("#FBF8FD")
                .build();
        themes.put("skullpanda", skullpanda);

        // 4. Jujutsu Kaisen (Chú Thuật Hồi Chiến)
        PopNowSeriesTheme jujutsu = PopNowSeriesTheme.builder()
                .seriesKey("jujutsu")
                .seriesName("Jujutsu Kaisen Series")
                .trayBgUrl("https://prod-global-biz.popmart.com/globalAdmin/1789803734051_c107a9b26eee8ea1e35e47974d0e507e.png")
                .smallBoxImgUrl("https://prod-global-biz.popmart.com/globalAdmin/1789803734049_ec105be95fcad037caf69a67dfe02d35.png")
                .trayFrontUrl("https://prod-global-biz.popmart.com/globalAdmin/1789803734050_91bb21168bdcb0b9f113d63a88f49b0f.png")
                .trayShadowUrl("/images/popnow/tray-shadow.png")
                .guideHandUrl("/images/popnow/hand-guide.png")
                .totalSlots(6)
                .primaryColor("#8B0000")
                .accentColor("#FF4D4F")
                .backgroundColor("#FAF6F6")
                .build();
        themes.put("jujutsu", jujutsu);

        // 5. Mega Space Molly
        PopNowSeriesTheme molly = PopNowSeriesTheme.builder()
                .seriesKey("molly")
                .seriesName("Mega Space Molly Series")
                .trayBgUrl("https://prod-global-biz.popmart.com/globalAdmin/1789803344214_c107a9b26eee8ea1e35e47974d0e507e.png")
                .smallBoxImgUrl("https://prod-global-biz.popmart.com/globalAdmin/1789803344213_ec105be95fcad037caf69a67dfe02d35.png")
                .trayFrontUrl("https://prod-global-biz.popmart.com/globalAdmin/1789803344213_91bb21168bdcb0b9f113d63a88f49b0f.png")
                .trayShadowUrl("/images/popnow/tray-shadow.png")
                .guideHandUrl("/images/popnow/hand-guide.png")
                .totalSlots(6)
                .primaryColor("#0B3C5D")
                .accentColor("#328CC1")
                .backgroundColor("#F4F8FA")
                .build();
        themes.put("molly", molly);

        // 6. Crybaby / Dimoo
        PopNowSeriesTheme crybaby = PopNowSeriesTheme.builder()
                .seriesKey("crybaby")
                .seriesName("Crybaby Crying Parade")
                .trayBgUrl("https://prod-global-biz.popmart.com/globalAdmin/1781855858883_c107a9b26eee8ea1e35e47974d0e507e.png")
                .smallBoxImgUrl("https://prod-global-biz.popmart.com/globalAdmin/1781855858883_ec105be95fcad037caf69a67dfe02d35.png")
                .trayFrontUrl("https://prod-global-biz.popmart.com/globalAdmin/1781855858883_91bb21168bdcb0b9f113d63a88f49b0f.png")
                .trayShadowUrl("/images/popnow/tray-shadow.png")
                .guideHandUrl("/images/popnow/hand-guide.png")
                .totalSlots(6)
                .primaryColor("#E84393")
                .accentColor("#FDCB6E")
                .backgroundColor("#FFF9FB")
                .build();
        themes.put("crybaby", crybaby);
        themes.put("dimoo", crybaby);

        this.defaultTheme = monsters;
    }

    @Override
    public PopNowSeriesTheme getThemeForProduct(Product product) {
        if (product == null) {
            return defaultTheme;
        }
        String slug = product.getSlug() != null ? product.getSlug().toLowerCase() : "";
        String name = product.getName() != null ? product.getName().toLowerCase() : "";

        if (slug.contains("hirono") || name.contains("hirono")) {
            return themes.get("hirono");
        }
        if (slug.contains("skullpanda") || name.contains("skullpanda")) {
            return themes.get("skullpanda");
        }
        if (slug.contains("jujutsu") || name.contains("jujutsu") || name.contains("chú thuật")) {
            return themes.get("jujutsu");
        }
        if (slug.contains("molly") || name.contains("molly")) {
            return themes.get("molly");
        }
        if (slug.contains("crybaby") || name.contains("crybaby")) {
            return themes.get("crybaby");
        }
        if (slug.contains("dimoo") || name.contains("dimoo")) {
            return themes.get("dimoo");
        }
        if (slug.contains("labubu") || slug.contains("monsters") || name.contains("labubu") || name.contains("monsters")) {
            return themes.get("monsters");
        }

        // Tự động fallback linh hoạt nếu sản phẩm chưa có theme riêng:
        // Sử dụng ảnh chính của sản phẩm làm hộp 3D và khay tiêu chuẩn PopWorld
        return PopNowSeriesTheme.builder()
                .seriesKey("default")
                .seriesName(product.getName())
                .trayBgUrl(defaultTheme.getTrayBgUrl())
                .smallBoxImgUrl(defaultTheme.getSmallBoxImgUrl())
                .trayFrontUrl(defaultTheme.getTrayFrontUrl())
                .trayShadowUrl(defaultTheme.getTrayShadowUrl())
                .guideHandUrl(defaultTheme.getGuideHandUrl())
                .totalSlots(6)
                .primaryColor("#D2001E")
                .accentColor("#F5C518")
                .backgroundColor("#F8FBFB")
                .build();
    }

    @Override
    public PopNowSeriesTheme getThemeBySlug(String slug) {
        if (slug == null) return defaultTheme;
        String s = slug.toLowerCase();
        for (Map.Entry<String, PopNowSeriesTheme> entry : themes.entrySet()) {
            if (s.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return defaultTheme;
    }

    @Override
    public Map<String, PopNowSeriesTheme> getAllThemes() {
        return Collections.unmodifiableMap(themes);
    }
}
