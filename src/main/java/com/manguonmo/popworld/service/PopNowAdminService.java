package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.BlindBoxItemFormRequest;
import com.manguonmo.popworld.dto.response.PopNowAdminProductSummary;
import com.manguonmo.popworld.entity.BlindBoxItem;
import com.manguonmo.popworld.entity.BlindBoxSlot;
import com.manguonmo.popworld.entity.Product;

import java.util.List;

public interface PopNowAdminService {

    List<PopNowAdminProductSummary> getPopNowProductSummaries();

    Product getProductForConfig(Long productId);

    List<BlindBoxItem> getItemsByProductId(Long productId);

    BlindBoxItem getItemById(Long productId, Long itemId);

    BlindBoxItem saveItem(Long productId, Long itemId, BlindBoxItemFormRequest request);

    void toggleItemActive(Long productId, Long itemId);

    void deleteItem(Long productId, Long itemId);

    List<BlindBoxSlot> getSlotsByProductId(Long productId);

    int initializeStandardSlots(Long productId);

    int initializeSlots(Long productId, Integer slotCount);

    void updateBoxesPerSet(Long productId, Integer boxesPerSet);

    int cleanupExcessAvailableSlots(Long productId, Integer targetCount);

    boolean isExistingSeriesWithStandardItems(Long productId);

    int syncSeriesStandardItems(Long productId, boolean overrideExisting);

    int quickGenerateTemplateItems(Long productId, Integer count);
}
