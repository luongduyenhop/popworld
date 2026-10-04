package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.BlindBoxItemFormRequest;
import com.manguonmo.popworld.dto.response.PopNowAdminProductSummary;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.BlindBoxItemRepository;
import com.manguonmo.popworld.repository.BlindBoxSlotRepository;
import com.manguonmo.popworld.repository.OwnedItemRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.service.PopNowAdminService;
import com.manguonmo.popworld.service.PopNowSeriesCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopNowAdminServiceImpl implements PopNowAdminService {

    private static final int STANDARD_SLOT_COUNT = 12;

    private final ProductRepository productRepository;
    private final BlindBoxItemRepository blindBoxItemRepository;
    private final BlindBoxSlotRepository blindBoxSlotRepository;
    private final OwnedItemRepository ownedItemRepository;
    private final PopNowSeriesCatalog popNowSeriesCatalog;

    @Override
    public List<PopNowAdminProductSummary> getPopNowProductSummaries() {
        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        return products.stream()
                .filter(p -> p.getCategory() != null &&
                        !"accessories".equalsIgnoreCase(p.getCategory().getSlug()) &&
                        !"mega-collection".equalsIgnoreCase(p.getCategory().getSlug()))
                .map(product -> {
            Long pId = product.getId();
            long totalItems = blindBoxItemRepository.findByProductId(pId).size();
            long activeItems = blindBoxItemRepository.countByProductIdAndActiveTrue(pId);
            long availableSlots = blindBoxSlotRepository.countByProductIdAndStatus(pId, SlotStatus.AVAILABLE);
            long heldSlots = blindBoxSlotRepository.countByProductIdAndStatus(pId, SlotStatus.HELD);
            long soldSlots = blindBoxSlotRepository.countByProductIdAndStatus(pId, SlotStatus.SOLD);
            long totalSlots = availableSlots + heldSlots + soldSlots;

            String catName = product.getCategory() != null ? product.getCategory().getName() : "Chưa phân loại";

            return PopNowAdminProductSummary.builder()
                    .productId(pId)
                    .productName(product.getName())
                    .productSlug(product.getSlug())
                    .categoryName(catName)
                    .productStock(product.getStockQuantity())
                    .productActive(product.getActive())
                    .totalItems(totalItems)
                    .activeItems(activeItems)
                    .availableSlots(availableSlots)
                    .heldSlots(heldSlots)
                    .soldSlots(soldSlots)
                    .totalSlots(totalSlots)
                    .boxesPerSet(product.getBoxesPerSet() != null ? product.getBoxesPerSet() : 12)
                    .build();
        }).toList();
    }

    @Override
    public Product getProductForConfig(Long productId) {
        if (productId == null) {
            throw new BadRequestException("ID sản phẩm không được để trống!");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));
    }

    @Override
    @Transactional
    public List<BlindBoxItem> getItemsByProductId(Long productId) {
        Product product = getProductForConfig(productId);
        List<BlindBoxItem> items = blindBoxItemRepository.findByProductId(productId);
        if (items.isEmpty() && isExistingSeriesWithStandardItems(productId)) {
            // Tự động nạp sẵn đủ các mô hình thuộc series đó nếu là bộ sưu tập đã tồn tại
            log.info("Phát hiện bộ sưu tập đã tồn tại có định nghĩa chuẩn. Tự động nạp danh sách mô hình cho productId={}", productId);
            syncSeriesStandardItems(productId, false);
            items = blindBoxItemRepository.findByProductId(productId);
        }
        return items;
    }

    @Override
    public BlindBoxItem getItemById(Long productId, Long itemId) {
        if (itemId == null) {
            throw new BadRequestException("ID mô hình không được để trống!");
        }
        BlindBoxItem item = blindBoxItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mô hình với ID: " + itemId));

        if (!item.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Mô hình không thuộc về sản phẩm này!");
        }
        return item;
    }

    @Override
    @Transactional
    public BlindBoxItem saveItem(Long productId, Long itemId, BlindBoxItemFormRequest request) {
        Product product = getProductForConfig(productId);

        if (request == null) {
            throw new BadRequestException("Dữ liệu cấu hình mô hình không hợp lệ!");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Tên mô hình không được để trống!");
        }
        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            throw new BadRequestException("Đường dẫn ảnh không được để trống!");
        }
        if (request.getProbabilityWeight() == null || request.getProbabilityWeight() <= 0) {
            throw new BadRequestException("Trọng số xác suất phải lớn hơn 0!");
        }
        if (request.getRarity() == null) {
            throw new BadRequestException("Vui lòng chọn cấp độ hiếm (Rarity)!");
        }

        BlindBoxItem item;
        if (itemId == null) {
            item = BlindBoxItem.builder()
                    .product(product)
                    .name(request.getName().trim())
                    .rarity(request.getRarity())
                    .imageUrl(request.getImageUrl().trim())
                    .probabilityWeight(request.getProbabilityWeight())
                    .stockQuantity(request.getStockQuantity())
                    .active(Boolean.TRUE.equals(request.getActive()))
                    .build();
        } else {
            item = getItemById(productId, itemId);

            // Ngăn chặn tắt active nếu đây là item hoạt động duy nhất
            if (Boolean.FALSE.equals(request.getActive()) && Boolean.TRUE.equals(item.getActive())) {
                List<BlindBoxItem> currentActives = blindBoxItemRepository.findByProductIdAndActiveTrue(productId);
                if (currentActives.size() <= 1 && currentActives.stream().anyMatch(i -> i.getId().equals(itemId))) {
                    throw new BadRequestException("Không thể tắt kích hoạt vì đây là mô hình hoạt động duy nhất của Series! Phải có ít nhất 1 mô hình đang hoạt động để phục vụ unbox.");
                }
            }

            item.setName(request.getName().trim());
            item.setRarity(request.getRarity());
            item.setImageUrl(request.getImageUrl().trim());
            item.setProbabilityWeight(request.getProbabilityWeight());
            item.setStockQuantity(request.getStockQuantity());
            item.setActive(Boolean.TRUE.equals(request.getActive()));
        }

        BlindBoxItem saved = blindBoxItemRepository.save(item);
        log.info("Admin đã lưu BlindBoxItem thành công: id={}, name={}, product={}, active={}",
                saved.getId(), saved.getName(), product.getName(), saved.getActive());
        return saved;
    }

    @Override
    @Transactional
    public void toggleItemActive(Long productId, Long itemId) {
        BlindBoxItem item = getItemById(productId, itemId);

        if (Boolean.TRUE.equals(item.getActive())) {
            // Đang bật mà muốn tắt -> kiểm tra xem có phải item duy nhất không
            List<BlindBoxItem> currentActives = blindBoxItemRepository.findByProductIdAndActiveTrue(productId);
            if (currentActives.size() <= 1 && currentActives.stream().anyMatch(i -> i.getId().equals(itemId))) {
                throw new BadRequestException("Không thể tắt kích hoạt mô hình duy nhất của Series! Phải có ít nhất 1 mô hình đang hoạt động.");
            }
            item.setActive(false);
        } else {
            item.setActive(true);
        }

        blindBoxItemRepository.save(item);
        log.info("Admin đã thay đổi trạng thái hoạt động BlindBoxItem id={}: active={}", itemId, item.getActive());
    }

    @Override
    @Transactional
    public void deleteItem(Long productId, Long itemId) {
        BlindBoxItem item = getItemById(productId, itemId);

        // Kiểm tra lịch sử khách hàng đã unbox trúng item này vào Virtual Cabinet
        if (ownedItemRepository.existsByBlindBoxItemId(itemId)) {
            throw new BadRequestException("Mô hình này đã có người chơi sở hữu trong tủ đồ ảo! Để đảm bảo toàn vẹn dữ liệu, bạn không thể xóa vĩnh viễn mô hình này. Vui lòng sử dụng tính năng Tắt kích hoạt (Deactivate).");
        }

        // Kiểm tra an toàn: Không xóa nếu là item active duy nhất
        List<BlindBoxItem> currentActives = blindBoxItemRepository.findByProductIdAndActiveTrue(productId);
        if (currentActives.size() <= 1 && currentActives.stream().anyMatch(i -> i.getId().equals(itemId))) {
            throw new BadRequestException("Không thể xóa mô hình duy nhất đang hoạt động của Series! Vui lòng thêm mô hình thay thế trước.");
        }

        blindBoxItemRepository.delete(item);
        log.info("Admin đã xóa an toàn BlindBoxItem id={} của sản phẩm productId={}", itemId, productId);
    }

    @Override
    public List<BlindBoxSlot> getSlotsByProductId(Long productId) {
        getProductForConfig(productId);
        return blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(productId);
    }

    @Override
    @Transactional
    public int initializeStandardSlots(Long productId) {
        return initializeSlots(productId, null);
    }

    @Override
    @Transactional
    public int initializeSlots(Long productId, Integer slotCount) {
        Product product = getProductForConfig(productId);
        if (product.getCategory() != null && "accessories".equalsIgnoreCase(product.getCategory().getSlug())) {
            throw new BadRequestException("Không thể khởi tạo ô hộp POP NOW cho sản phẩm thuộc danh mục Phụ kiện!");
        }

        int targetSlots = (slotCount != null && slotCount > 0) ? slotCount :
                (product.getBoxesPerSet() != null && product.getBoxesPerSet() > 0 ? product.getBoxesPerSet() : 12);

        if (product.getBoxesPerSet() == null || !product.getBoxesPerSet().equals(targetSlots)) {
            product.setBoxesPerSet(targetSlots);
            productRepository.save(product);
        }

        int createdCount = 0;
        for (int slotIdx = 1; slotIdx <= targetSlots; slotIdx++) {
            Optional<BlindBoxSlot> existingSlot = blindBoxSlotRepository.findByProductIdAndSlotIndex(productId, slotIdx);
            if (existingSlot.isEmpty()) {
                BlindBoxSlot newSlot = BlindBoxSlot.builder()
                        .product(product)
                        .slotIndex(slotIdx)
                        .status(SlotStatus.AVAILABLE)
                        .build();
                blindBoxSlotRepository.save(newSlot);
                createdCount++;
            }
        }

        log.info("Admin đã khởi tạo {} ô hộp mới (quy cách {} ô) cho sản phẩm ID={}", createdCount, targetSlots, productId);
        return createdCount;
    }

    @Override
    @Transactional
    public void updateBoxesPerSet(Long productId, Integer boxesPerSet) {
        if (boxesPerSet == null || boxesPerSet <= 0) {
            throw new BadRequestException("Số lượng hộp trong 1 bộ phải lớn hơn 0!");
        }
        Product product = getProductForConfig(productId);
        product.setBoxesPerSet(boxesPerSet);
        productRepository.save(product);
        log.info("Admin đã cập nhật quy cách bộ hộp productId={} thành {} hộp/set", productId, boxesPerSet);
    }

    @Override
    @Transactional
    public int cleanupExcessAvailableSlots(Long productId, Integer targetCount) {
        if (targetCount == null || targetCount <= 0) {
            throw new BadRequestException("Số lượng hộp mục tiêu không hợp lệ!");
        }
        Product product = getProductForConfig(productId);
        List<BlindBoxSlot> slots = blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(productId);
        int deletedCount = 0;
        for (BlindBoxSlot slot : slots) {
            if (slot.getSlotIndex() > targetCount && slot.getStatus() == SlotStatus.AVAILABLE) {
                blindBoxSlotRepository.delete(slot);
                deletedCount++;
            }
        }
        log.info("Admin đã dọn dẹp {} ô trống vượt quá quy cách {} ô cho sản phẩm ID={}", deletedCount, targetCount, productId);
        return deletedCount;
    }

    @Override
    public boolean isExistingSeriesWithStandardItems(Long productId) {
        Product product = getProductForConfig(productId);
        String seriesName = product.getSeries() != null ? product.getSeries().getName() : null;

        // 1. Kiểm tra catalog chuẩn POP MART
        if (popNowSeriesCatalog.hasCatalog(product.getSlug(), seriesName, product.getName())) {
            return true;
        }

        // 2. Kiểm tra xem có sản phẩm nào khác cùng Series đã có mô hình chưa
        if (product.getSeries() != null) {
            List<Product> siblingProducts = productRepository.findBySeriesId(product.getSeries().getId());
            for (Product sib : siblingProducts) {
                if (!sib.getId().equals(productId) && !blindBoxItemRepository.findByProductId(sib.getId()).isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    @Transactional
    public int syncSeriesStandardItems(Long productId, boolean overrideExisting) {
        Product product = getProductForConfig(productId);
        String seriesName = product.getSeries() != null ? product.getSeries().getName() : null;
        List<BlindBoxItem> existingItems = blindBoxItemRepository.findByProductId(productId);
        Map<String, BlindBoxItem> existingByName = new HashMap<>();
        for (BlindBoxItem item : existingItems) {
            existingByName.put(item.getName().trim().toLowerCase(), item);
        }

        List<PopNowSeriesCatalog.StandardItemDef> defsToSync = new ArrayList<>();
        int targetBoxesPerSet = product.getBoxesPerSet() != null ? product.getBoxesPerSet() : 12;

        // 1. Ưu tiên kiểm tra catalog chuẩn
        Optional<PopNowSeriesCatalog.SeriesCatalogDef> catalogDef = popNowSeriesCatalog.findCatalog(product.getSlug(), seriesName, product.getName());
        if (catalogDef.isPresent()) {
            defsToSync.addAll(catalogDef.get().getItems());
            targetBoxesPerSet = catalogDef.get().getBoxesPerSet();
        } else if (product.getSeries() != null) {
            // 2. Kiểm tra sản phẩm cùng series đã có items
            List<Product> siblingProducts = productRepository.findBySeriesId(product.getSeries().getId());
            for (Product sib : siblingProducts) {
                if (!sib.getId().equals(productId)) {
                    List<BlindBoxItem> sibItems = blindBoxItemRepository.findByProductId(sib.getId());
                    if (!sibItems.isEmpty()) {
                        for (BlindBoxItem sItem : sibItems) {
                            defsToSync.add(new PopNowSeriesCatalog.StandardItemDef(
                                    sItem.getName(),
                                    sItem.getRarity(),
                                    sItem.getImageUrl(),
                                    sItem.getProbabilityWeight() != null ? sItem.getProbabilityWeight() : 100
                            ));
                        }
                        if (sib.getBoxesPerSet() != null) {
                            targetBoxesPerSet = sib.getBoxesPerSet();
                        }
                        break;
                    }
                }
            }
        }

        if (defsToSync.isEmpty()) {
            log.warn("Không tìm thấy định nghĩa mô hình chuẩn cho Series của productId={}", productId);
            return 0;
        }

        // Cập nhật quy cách đóng gói nếu khác
        if (product.getBoxesPerSet() == null || !product.getBoxesPerSet().equals(targetBoxesPerSet)) {
            product.setBoxesPerSet(targetBoxesPerSet);
            productRepository.save(product);
        }

        int savedCount = 0;
        String fallbackImg = (product.getMainImageUrl() != null && !product.getMainImageUrl().isBlank())
                ? product.getMainImageUrl()
                : ((product.getImages() != null && !product.getImages().isEmpty())
                    ? product.getImages().get(0).getImageUrl()
                    : "/images/placeholder.svg");

        for (PopNowSeriesCatalog.StandardItemDef def : defsToSync) {
            String key = def.name().trim().toLowerCase();
            BlindBoxItem existing = existingByName.get(key);

            if (existing == null) {
                BlindBoxItem newItem = BlindBoxItem.builder()
                        .product(product)
                        .name(def.name())
                        .rarity(def.rarity())
                        .imageUrl(def.imageUrl() != null && !def.imageUrl().isBlank() ? def.imageUrl() : fallbackImg)
                        .probabilityWeight(def.weight() > 0 ? def.weight() : (def.rarity() == RarityType.SECRET ? 10 : 100))
                        .active(true)
                        .build();
                blindBoxItemRepository.save(newItem);
                savedCount++;
            } else if (overrideExisting) {
                existing.setRarity(def.rarity());
                if (def.imageUrl() != null && !def.imageUrl().isBlank()) {
                    existing.setImageUrl(def.imageUrl());
                }
                existing.setProbabilityWeight(def.weight() > 0 ? def.weight() : (def.rarity() == RarityType.SECRET ? 10 : 100));
                existing.setActive(true);
                blindBoxItemRepository.save(existing);
                savedCount++;
            }
        }

        // Tự động khởi tạo khay ô hộp nếu chưa có
        List<BlindBoxSlot> slots = blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(productId);
        if (slots.isEmpty()) {
            initializeSlots(productId, targetBoxesPerSet);
        }

        log.info("Đã đồng bộ {} mô hình chuẩn cho productId={}", savedCount, productId);
        return savedCount;
    }

    @Override
    @Transactional
    public int quickGenerateTemplateItems(Long productId, Integer count) {
        Product product = getProductForConfig(productId);
        int total = (count != null && count > 0) ? count : (product.getBoxesPerSet() != null ? product.getBoxesPerSet() : 12);

        String fallbackImg = (product.getMainImageUrl() != null && !product.getMainImageUrl().isBlank())
                ? product.getMainImageUrl()
                : ((product.getImages() != null && !product.getImages().isEmpty())
                    ? product.getImages().get(0).getImageUrl()
                    : "/images/placeholder.svg");

        List<BlindBoxItem> existing = blindBoxItemRepository.findByProductId(productId);
        int startIndex = existing.size() + 1;
        int created = 0;

        for (int i = 1; i <= total; i++) {
            boolean isSecret = (i == total); // Mô hình cuối là Secret
            String itemName = isSecret ? "Mẫu Bí Mật (Secret Edition)" : ("Nhân vật #" + (startIndex + i - 1));
            RarityType rarity = isSecret ? RarityType.SECRET : RarityType.REGULAR;
            int weight = isSecret ? 10 : 100;

            BlindBoxItem item = BlindBoxItem.builder()
                    .product(product)
                    .name(itemName)
                    .rarity(rarity)
                    .imageUrl(fallbackImg)
                    .probabilityWeight(weight)
                    .active(true)
                    .build();
            blindBoxItemRepository.save(item);
            created++;
        }

        // Tự động tạo ô hộp nếu chưa có
        List<BlindBoxSlot> slots = blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(productId);
        if (slots.isEmpty()) {
            initializeSlots(productId, total);
        }

        log.info("Đã tạo nhanh {} mô hình mẫu cho bộ sưu tập mới productId={}", created, productId);
        return created;
    }
}
