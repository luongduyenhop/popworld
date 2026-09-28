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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    @Override
    public List<PopNowAdminProductSummary> getPopNowProductSummaries() {
        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        return products.stream().map(product -> {
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
    public List<BlindBoxItem> getItemsByProductId(Long productId) {
        getProductForConfig(productId);
        return blindBoxItemRepository.findByProductId(productId);
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
        Product product = getProductForConfig(productId);
        int createdCount = 0;

        for (int slotIdx = 1; slotIdx <= STANDARD_SLOT_COUNT; slotIdx++) {
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
            // Tuyệt đối không reset hay sửa đổi slot đã tồn tại (đặc biệt HELD hoặc SOLD)
        }

        log.info("Admin đã khởi tạo {} ô hộp tiêu chuẩn mới cho sản phẩm ID={}", createdCount, productId);
        return createdCount;
    }
}
