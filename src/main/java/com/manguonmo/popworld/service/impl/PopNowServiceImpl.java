package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.response.BlindBoxItemResponse;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.*;
import com.manguonmo.popworld.service.PopNowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopNowServiceImpl implements PopNowService {

    // POP MART benchmark: Pick a Box reservation is 5 minutes
    private static final int RESERVATION_TTL_MINUTES = 5;
    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BlindBoxItemRepository blindBoxItemRepository;
    private final BoxReservationRepository boxReservationRepository;
    private final OwnedItemRepository ownedItemRepository;
    private final BlindBoxSlotRepository blindBoxSlotRepository;

    @Override
    @Transactional
    public BoxReservationResponse reserveBox(Long userId, BoxReservationRequest request) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực tài khoản để giữ hộp!");
        }
        if (request == null || request.getProductId() == null) {
            throw new BadRequestException("Thông tin giữ hộp không hợp lệ!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + request.getProductId()));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("Sản phẩm hiện đang tạm dừng mở bán!");
        }

        Integer boxIndex = request.getBoxIndex();
        BlindBoxSlot slot = null;

        // DB-backed slot exclusivity and pessimistic row locking
        if (boxIndex != null) {
            slot = getOrCreateSlotForUpdate(product, boxIndex);
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                throw new BadRequestException("Vị trí hộp số " + boxIndex + " hiện đang được người khác giữ hoặc đã bán!");
            }
        }

        // Kiểm tra và trừ tồn kho nguyên tử (tránh race condition làm âm kho)
        int updated = productRepository.updateStock(product.getId(), 1);
        if (updated == 0) {
            throw new BadRequestException("Sản phẩm đã hết hàng, không thể giữ hộp!");
        }

        LocalDateTime now = LocalDateTime.now();
        String code = "PN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        BoxReservation reservation = BoxReservation.builder()
                .user(user)
                .product(product)
                .slot(slot)
                .reservationCode(code)
                .boxIndex(boxIndex)
                .status(ReservationStatus.RESERVED)
                .reservedAt(now)
                .expiresAt(now.plusMinutes(RESERVATION_TTL_MINUTES))
                .build();

        reservation = boxReservationRepository.save(reservation);

        if (slot != null) {
            slot.setStatus(SlotStatus.HELD);
            slot.setCurrentReservation(reservation);
            blindBoxSlotRepository.save(slot);
        }

        log.info("Khách hàng ID={} đã giữ hộp thành công (TTL {} phút): code={}, product={}, boxIndex={}",
                userId, RESERVATION_TTL_MINUTES, code, product.getName(), boxIndex);

        return mapToReservationResponse(reservation);
    }

    @Override
    @Transactional
    public void cancelReservation(Long userId, String reservationCode) {
        if (reservationCode == null || reservationCode.isBlank()) {
            throw new BadRequestException("Mã giữ hộp không hợp lệ!");
        }

        BoxReservation reservation = boxReservationRepository.findByReservationCode(reservationCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu giữ hộp với mã: " + reservationCode));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thao tác trên phiếu giữ hộp này!");
        }

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new BadRequestException("Chỉ có thể hủy phiếu giữ hộp đang ở trạng thái RESERVED!");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        boxReservationRepository.save(reservation);

        // Giải phóng slot về AVAILABLE
        BlindBoxSlot slot = reservation.getSlot();
        if (slot == null && reservation.getBoxIndex() != null) {
            slot = blindBoxSlotRepository.findByProductIdAndSlotIndex(reservation.getProduct().getId(), reservation.getBoxIndex()).orElse(null);
        }
        if (slot != null) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slot.setCurrentReservation(null);
            blindBoxSlotRepository.save(slot);
        }

        // Hoàn lại 1 tồn kho vào sản phẩm
        productRepository.addStock(reservation.getProduct().getId(), 1);
        log.info("Khách hàng ID={} đã chủ động hủy phiếu giữ hộp code={}. Đã hoàn lại 1 tồn kho và mở lại slot.", userId, reservationCode);
    }

    @Override
    @Transactional
    public void markPurchased(String reservationCode, String orderCode) {
        if (reservationCode == null || reservationCode.isBlank()) {
            throw new BadRequestException("Mã giữ hộp không hợp lệ!");
        }
        BoxReservation reservation = boxReservationRepository.findByReservationCode(reservationCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu giữ hộp: " + reservationCode));

        if (reservation.getStatus() == ReservationStatus.EXPIRED || reservation.isExpired()) {
            if (reservation.getStatus() == ReservationStatus.RESERVED) {
                reservation.setStatus(ReservationStatus.EXPIRED);
                boxReservationRepository.save(reservation);
                productRepository.addStock(reservation.getProduct().getId(), 1);

                BlindBoxSlot slot = reservation.getSlot();
                if (slot == null && reservation.getBoxIndex() != null) {
                    slot = blindBoxSlotRepository.findByProductIdAndSlotIndex(reservation.getProduct().getId(), reservation.getBoxIndex()).orElse(null);
                }
                if (slot != null) {
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slot.setCurrentReservation(null);
                    blindBoxSlotRepository.save(slot);
                }
            }
            throw new BadRequestException("Phiếu giữ hộp đã hết hạn, không thể thanh toán!");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Phiếu giữ hộp đã bị hủy, không thể thanh toán!");
        }

        if (reservation.getStatus() == ReservationStatus.UNBOXED || reservation.getStatus() == ReservationStatus.PURCHASED) {
            throw new BadRequestException("Phiếu giữ hộp đã được thanh toán hoặc đã mở trước đó!");
        }

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new BadRequestException("Trạng thái phiếu giữ hộp không hợp lệ để thanh toán!");
        }

        reservation.setStatus(ReservationStatus.PURCHASED);
        reservation.setOrderCode(orderCode);
        boxReservationRepository.save(reservation);

        // Chuyển slot sang SOLD
        BlindBoxSlot slot = reservation.getSlot();
        if (slot == null && reservation.getBoxIndex() != null) {
            slot = blindBoxSlotRepository.findByProductIdAndSlotIndex(reservation.getProduct().getId(), reservation.getBoxIndex()).orElse(null);
        }
        if (slot != null) {
            slot.setStatus(SlotStatus.SOLD);
            blindBoxSlotRepository.save(slot);
        }

        log.info("Phiếu giữ hộp code={} đã chuyển sang trạng thái PURCHASED với orderCode={}", reservationCode, orderCode);
    }

    @Override
    @Transactional
    public OwnedItemResponse unbox(Long userId, String reservationCode) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực tài khoản để mở hộp!");
        }
        if (reservationCode == null || reservationCode.isBlank()) {
            throw new BadRequestException("Mã giữ hộp không được để trống!");
        }

        // Khóa bi quan BoxReservation để tuần tự hóa (serialize) các yêu cầu mở đồng thời
        BoxReservation reservation = boxReservationRepository.findByReservationCodeForUpdate(reservationCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu giữ hộp với mã: " + reservationCode));

        // Kiểm tra quyền sở hữu IDOR
        if (!reservation.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền mở hộp với mã phiếu này!");
        }

        // Idempotency: Nếu hộp này đã được unbox trước đó, trả về đúng vật phẩm đã mở (không sinh thêm)
        if (reservation.getStatus() == ReservationStatus.UNBOXED) {
            OwnedItem existingItem = ownedItemRepository.findByReservationId(reservation.getId())
                    .orElse(null);
            if (existingItem != null) {
                log.info("Hộp code={} đã được mở trước đó. Trả về kết quả hiện tại (Idempotent).", reservationCode);
                return mapToOwnedItemResponse(existingItem);
            }
        }

        // UNBOX must reject RESERVED/HELD; payment must happen first!
        if (reservation.getStatus() == ReservationStatus.RESERVED) {
            throw new BadRequestException("Hộp chưa được thanh toán! Vui lòng hoàn tất thanh toán trước khi mở hộp.");
        }

        // Kiểm tra trạng thái hợp lệ để bốc
        if (reservation.getStatus() == ReservationStatus.EXPIRED || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Phiếu giữ hộp đã quá hạn hoặc bị hủy, không thể mở hộp!");
        }

        // Chỉ trạng thái PURCHASED mới được phép mở hộp
        if (reservation.getStatus() != ReservationStatus.PURCHASED) {
            throw new BadRequestException("Trạng thái phiếu giữ hộp không hợp lệ để mở!");
        }

        // SERVER-SIDE AUTHORITY RANDOMIZATION:
        // Lấy danh sách nhân vật có thể mở trúng trong Series của Product
        List<BlindBoxItem> possibleItems = blindBoxItemRepository.findByProductIdAndActiveTrue(reservation.getProduct().getId());
        if (possibleItems == null || possibleItems.isEmpty()) {
            // Missing series configuration is a configuration error; fail safely without consuming/marking reservation
            throw new BadRequestException("Chưa cấu hình danh sách mô hình (BlindBoxItem) cho series này!");
        }

        // Thuật toán Weighted Random chọn nhân vật theo xác suất
        BlindBoxItem selectedItem = selectRandomItem(possibleItems);

        // Chuyển trạng thái phiếu sang UNBOXED
        reservation.setStatus(ReservationStatus.UNBOXED);
        boxReservationRepository.save(reservation);

        // Đảm bảo slot chuyển thành SOLD
        BlindBoxSlot slot = reservation.getSlot();
        if (slot == null && reservation.getBoxIndex() != null) {
            slot = blindBoxSlotRepository.findByProductIdAndSlotIndex(reservation.getProduct().getId(), reservation.getBoxIndex()).orElse(null);
        }
        if (slot != null && slot.getStatus() != SlotStatus.SOLD) {
            slot.setStatus(SlotStatus.SOLD);
            blindBoxSlotRepository.save(slot);
        }

        // Lưu vào tủ đồ cá nhân (Virtual Cabinet)
        OwnedItem ownedItem = OwnedItem.builder()
                .user(reservation.getUser())
                .blindBoxItem(selectedItem)
                .product(reservation.getProduct())
                .reservation(reservation)
                .orderCode(reservation.getOrderCode())
                .status(OwnedItemStatus.IN_CABINET)
                .unboxedAt(LocalDateTime.now())
                .build();

        ownedItem = ownedItemRepository.save(ownedItem);
        log.info("Mở hộp thành công: User ID={}, Box Code={}, Result={} ({})",
                userId, reservationCode, selectedItem.getName(), selectedItem.getRarity());

        return mapToOwnedItemResponse(ownedItem);
    }

    @Override
    public List<OwnedItemResponse> getUserCabinet(Long userId) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu mã định danh người dùng!");
        }
        return ownedItemRepository.findByUserIdOrderByUnboxedAtDesc(userId).stream()
                .map(this::mapToOwnedItemResponse)
                .toList();
    }

    @Override
    public List<BlindBoxItemResponse> getSeriesItems(Long productId) {
        if (productId == null) {
            throw new BadRequestException("ID sản phẩm không được rỗng!");
        }
        return blindBoxItemRepository.findByProductIdAndActiveTrue(productId).stream()
                .map(item -> BlindBoxItemResponse.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .rarity(item.getRarity().name())
                        .imageUrl(item.getImageUrl())
                        .isSecret(item.getRarity() == RarityType.SECRET)
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public int releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<BoxReservation> expiredList = boxReservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.RESERVED, now
        );

        int count = 0;
        for (BoxReservation res : expiredList) {
            res.setStatus(ReservationStatus.EXPIRED);
            boxReservationRepository.save(res);

            // Giải phóng slot về AVAILABLE
            BlindBoxSlot slot = res.getSlot();
            if (slot == null && res.getBoxIndex() != null) {
                slot = blindBoxSlotRepository.findByProductIdAndSlotIndex(res.getProduct().getId(), res.getBoxIndex()).orElse(null);
            }
            if (slot != null) {
                slot.setStatus(SlotStatus.AVAILABLE);
                slot.setCurrentReservation(null);
                blindBoxSlotRepository.save(slot);
            }

            productRepository.addStock(res.getProduct().getId(), 1);
            count++;
        }

        if (count > 0) {
            log.info("Đã giải phóng {} phiếu giữ hộp POP NOW hết hạn (TTL {} phút) và hoàn lại tồn kho, slot tương ứng.",
                    count, RESERVATION_TTL_MINUTES);
        }
        return count;
    }

    /**
     * Tìm hoặc khởi tạo ô hộp trong set với PESSIMISTIC_WRITE lock
     */
    private BlindBoxSlot getOrCreateSlotForUpdate(Product product, Integer boxIndex) {
        Optional<BlindBoxSlot> slotOpt = blindBoxSlotRepository.findByProductIdAndSlotIndexForUpdate(product.getId(), boxIndex);
        if (slotOpt.isPresent()) {
            return slotOpt.get();
        }
        BlindBoxSlot newSlot = BlindBoxSlot.builder()
                .product(product)
                .slotIndex(boxIndex)
                .status(SlotStatus.AVAILABLE)
                .build();
        try {
            return blindBoxSlotRepository.saveAndFlush(newSlot);
        } catch (Exception e) {
            return blindBoxSlotRepository.findByProductIdAndSlotIndexForUpdate(product.getId(), boxIndex)
                    .orElseThrow(() -> new BadRequestException("Vị trí hộp số " + boxIndex + " hiện đang được người khác giữ hoặc đã bán!"));
        }
    }

    /**
     * Thuật toán Weighted Random chọn ngẫu nhiên có trọng số xác suất
     */
    private BlindBoxItem selectRandomItem(List<BlindBoxItem> items) {
        int totalWeight = items.stream().mapToInt(BlindBoxItem::getProbabilityWeight).sum();
        if (totalWeight <= 0) {
            return items.get(secureRandom.nextInt(items.size()));
        }

        int randomValue = secureRandom.nextInt(totalWeight);
        int currentSum = 0;
        for (BlindBoxItem item : items) {
            currentSum += item.getProbabilityWeight();
            if (randomValue < currentSum) {
                return item;
            }
        }
        return items.get(0);
    }

    private BoxReservationResponse mapToReservationResponse(BoxReservation res) {
        return BoxReservationResponse.builder()
                .reservationCode(res.getReservationCode())
                .productId(res.getProduct().getId())
                .productName(res.getProduct().getName())
                .boxIndex(res.getBoxIndex())
                .status(res.getStatus().name())
                .reservedAt(res.getReservedAt())
                .expiresAt(res.getExpiresAt())
                .price(res.getProduct().getSinglePrice())
                .build();
    }

    private OwnedItemResponse mapToOwnedItemResponse(OwnedItem item) {
        return OwnedItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .itemId(item.getBlindBoxItem().getId())
                .itemName(item.getBlindBoxItem().getName())
                .rarity(item.getBlindBoxItem().getRarity().name())
                .imageUrl(item.getBlindBoxItem().getImageUrl())
                .status(item.getStatus().name())
                .unboxedAt(item.getUnboxedAt())
                .reservationCode(item.getReservation() != null ? item.getReservation().getReservationCode() : null)
                .build();
    }
}
