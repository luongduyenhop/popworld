package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.request.ShipCabinetRequest;
import com.manguonmo.popworld.dto.response.BlindBoxItemResponse;
import com.manguonmo.popworld.dto.response.BlindBoxSlotResponse;
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

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final UserAddressRepository userAddressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

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

        // Khóa bi quan hàng BoxReservation để tuần tự hóa cancel với scheduler/payment
        BoxReservation reservation = boxReservationRepository.findByReservationCodeForUpdate(reservationCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu giữ hộp với mã: " + reservationCode));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thao tác trên phiếu giữ hộp này!");
        }

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new BadRequestException("Chỉ có thể hủy phiếu giữ hộp đang ở trạng thái RESERVED!");
        }

        if (reservation.isExpired()) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            boxReservationRepository.save(reservation);
            releaseSlotAndRestoreStock(reservation);
            throw new BadRequestException("Phiếu giữ hộp đã hết thời gian hiệu lực (5 phút)! Không thể hủy.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        boxReservationRepository.save(reservation);

        // Giải phóng slot về AVAILABLE và hoàn lại tồn kho đúng 1 lần duy nhất
        releaseSlotAndRestoreStock(reservation);
        log.info("Khách hàng ID={} đã chủ động hủy phiếu giữ hộp code={}. Đã hoàn lại 1 tồn kho và mở lại slot.", userId, reservationCode);
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public void markPurchased(String reservationCode, String orderCode) {
        if (reservationCode == null || reservationCode.isBlank()) {
            throw new BadRequestException("Mã giữ hộp không hợp lệ!");
        }
        // Khóa bi quan hàng BoxReservation để tuần tự hóa với cancel/expiry và các webhook đồng thời
        BoxReservation reservation = boxReservationRepository.findByReservationCodeForUpdate(reservationCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu giữ hộp: " + reservationCode));

        // Tính lũy đẳng cho callback thanh toán lặp lại (Idempotent payment callback)
        if (reservation.getStatus() == ReservationStatus.PURCHASED) {
            log.info("Phiếu giữ hộp code={} đã ở trạng thái PURCHASED. Bỏ qua xử lý lặp lại (Idempotent).", reservationCode);
            return;
        }
        if (reservation.getStatus() == ReservationStatus.UNBOXED) {
            log.info("Phiếu giữ hộp code={} đã ở trạng thái UNBOXED. Bỏ qua xử lý lặp lại (Idempotent).", reservationCode);
            return;
        }

        if (reservation.getStatus() == ReservationStatus.EXPIRED || reservation.isExpired()) {
            if (reservation.getStatus() == ReservationStatus.RESERVED) {
                reservation.setStatus(ReservationStatus.EXPIRED);
                boxReservationRepository.save(reservation);
                releaseSlotAndRestoreStock(reservation);
            }
            throw new BadRequestException("Phiếu giữ hộp đã hết hạn, không thể thanh toán!");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Phiếu giữ hộp đã bị hủy, không thể thanh toán!");
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
            possibleItems = initializeDefaultSeriesItems(reservation.getProduct());
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
    @Transactional
    public List<BlindBoxItemResponse> getSeriesItems(Long productId) {
        if (productId == null) {
            throw new BadRequestException("ID sản phẩm không được rỗng!");
        }
        List<BlindBoxItem> items = blindBoxItemRepository.findByProductIdAndActiveTrue(productId);
        if (items.isEmpty()) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                items = initializeDefaultSeriesItems(product);
            }
        }
        return items.stream()
                .map(item -> BlindBoxItemResponse.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .rarity(item.getRarity().name())
                        .imageUrl(item.getImageUrl())
                        .isSecret(item.getRarity() == RarityType.SECRET)
                        .build())
                .toList();
    }

    /**
     * Tự động khởi tạo danh sách mô hình đặc trưng (BlindBoxItem) cho từng Series chuẩn POP MART
     */
    @Transactional
    protected List<BlindBoxItem> initializeDefaultSeriesItems(Product product) {
        if (product == null) {
            return Collections.emptyList();
        }
        String slug = product.getSlug() != null ? product.getSlug().toLowerCase() : "";
        String name = product.getName() != null ? product.getName().toLowerCase() : "";
        String fallbackImg = product.getMainImageUrl() != null && !product.getMainImageUrl().isBlank()
                ? product.getMainImageUrl()
                : "/images/popnow/small-box.png";

        String[] charNames;
        String secretName;

        if (slug.contains("labubu") || name.contains("labubu") || slug.contains("monsters") || name.contains("monsters")) {
            charNames = new String[]{"Camp Fire Labubu", "Fisherman Labubu", "Hiker Labubu", "Gardener Labubu", "Picnic Labubu", "Explorer Labubu"};
            secretName = "Golden Forest King (SECRET)";
        } else if (slug.contains("hirono") || name.contains("hirono")) {
            charNames = new String[]{"The Monster", "Destroyer", "Ragpicker", "Pretender", "Boiling Point", "Birdman"};
            secretName = "The Ghost (SECRET)";
        } else if (slug.contains("skullpanda") || name.contains("skullpanda")) {
            charNames = new String[]{"Lawyer", "City Police", "Dancer", "Bartender", "Puppeteer", "Navigator"};
            secretName = "Night Mayor (SECRET)";
        } else if (slug.contains("molly") || name.contains("molly")) {
            charNames = new String[]{"Molly Melty", "Molly Galaxy", "Molly Glacier", "Molly Retro", "Molly Rainbow", "Molly Neon"};
            secretName = "Molly Supernova (SECRET)";
        } else if (slug.contains("crybaby") || name.contains("crybaby")) {
            charNames = new String[]{"Crying Clown", "Teary Balloon", "Sad Drummer", "Melancholy Trumpet", "Raindrop Acrobat", "Weeping Jester"};
            secretName = "Golden Tear King (SECRET)";
        } else if (slug.contains("dimoo") || name.contains("dimoo")) {
            charNames = new String[]{"Retro Boy", "Vinyl Collector", "Arcade Gamer", "Tape Master", "Roller Skater", "Neon Dreamer"};
            secretName = "Golden Cassette (SECRET)";
        } else if (slug.contains("jujutsu") || name.contains("jujutsu") || name.contains("chú thuật")) {
            charNames = new String[]{"Yuji Itadori", "Megumi Fushiguro", "Nobara Kugisaki", "Satoru Gojo", "Kento Nanami", "Toge Inumaki"};
            secretName = "Ryomen Sukuna (SECRET)";
        } else {
            charNames = new String[]{"Loyalty", "Hope", "Love", "Luck", "Happiness", "Serenity"};
            secretName = "Golden Miracle (SECRET)";
        }

        List<BlindBoxItem> createdList = new ArrayList<>();
        for (String cName : charNames) {
            BlindBoxItem item = BlindBoxItem.builder()
                    .product(product)
                    .name(cName)
                    .imageUrl(fallbackImg)
                    .rarity(RarityType.REGULAR)
                    .probabilityWeight(100)
                    .active(true)
                    .build();
            createdList.add(blindBoxItemRepository.save(item));
        }

        BlindBoxItem secretItem = BlindBoxItem.builder()
                .product(product)
                .name(secretName)
                .imageUrl(fallbackImg)
                .rarity(RarityType.SECRET)
                .probabilityWeight(10)
                .active(true)
                .build();
        createdList.add(blindBoxItemRepository.save(secretItem));

        log.info("PopNowService: Đã tự động tạo {} BlindBoxItem cho series: {}", createdList.size(), product.getName());
        return createdList;
    }

    @Override
    @Transactional
    public int releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<BoxReservation> expiredCandidates = boxReservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.RESERVED, now
        );

        int count = 0;
        for (BoxReservation candidate : expiredCandidates) {
            // Khóa bi quan từng hàng để tránh race condition với cancel/payment đồng thời
            BoxReservation res = boxReservationRepository.findByIdForUpdate(candidate.getId())
                    .orElse(candidate);

            // Re-check trạng thái dưới khóa bi quan: nếu luồng khác đã cancel/purchase thì bỏ qua
            if (res.getStatus() != ReservationStatus.RESERVED) {
                continue;
            }

            res.setStatus(ReservationStatus.EXPIRED);
            boxReservationRepository.save(res);

            // Giải phóng slot về AVAILABLE và hoàn tồn kho đúng 1 lần duy nhất
            releaseSlotAndRestoreStock(res);
            count++;
        }

        if (count > 0) {
            log.info("Đã giải phóng {} phiếu giữ hộp POP NOW hết hạn (TTL {} phút) và hoàn lại tồn kho, slot tương ứng.",
                    count, RESERVATION_TTL_MINUTES);
        }
        return count;
    }

    /**
     * Giải phóng ô hộp về AVAILABLE và hoàn lại 1 tồn kho vào sản phẩm đúng 1 lần duy nhất
     */
    private void releaseSlotAndRestoreStock(BoxReservation res) {
        BlindBoxSlot slot = res.getSlot();
        if (slot == null && res.getBoxIndex() != null) {
            slot = blindBoxSlotRepository.findByProductIdAndSlotIndex(res.getProduct().getId(), res.getBoxIndex()).orElse(null);
        }
        if (slot != null && slot.getStatus() == SlotStatus.HELD) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slot.setCurrentReservation(null);
            blindBoxSlotRepository.save(slot);
        }

        productRepository.addStock(res.getProduct().getId(), 1);
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

    @Override
    public List<BlindBoxSlotResponse> getProductSlots(Long productId) {
        if (productId == null) {
            throw new BadRequestException("ID sản phẩm không được để trống!");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        List<BlindBoxSlot> existingSlots = blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(productId);
        Map<Integer, BlindBoxSlot> slotMap = existingSlots.stream()
                .collect(Collectors.toMap(BlindBoxSlot::getSlotIndex, s -> s, (s1, s2) -> s1));

        int totalSlots = 12; // Mặc định 12 ô chuẩn POP MART
        if (product.getPackagingType() != null && !product.getPackagingType().isBlank()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*(?:box|hộp|mẫu|piece|case|slot)", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(product.getPackagingType());
            int foundCount = 0;
            while (m.find()) {
                int count = Integer.parseInt(m.group(1));
                if (count > 1) {
                    foundCount = count;
                }
            }
            if (foundCount > 0) {
                totalSlots = foundCount;
            }
        }

        int maxIndex = existingSlots.stream()
                .mapToInt(BlindBoxSlot::getSlotIndex)
                .max()
                .orElse(0);
        totalSlots = Math.max(totalSlots, maxIndex);

        List<BlindBoxSlotResponse> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= totalSlots; i++) {
            BlindBoxSlot slot = slotMap.get(i);
            String status = "AVAILABLE";
            if (slot != null) {
                if (slot.getStatus() == SlotStatus.SOLD) {
                    status = "SOLD";
                } else if (slot.getStatus() == SlotStatus.HELD) {
                    if (slot.getCurrentReservation() != null && slot.getCurrentReservation().getExpiresAt() != null
                            && now.isAfter(slot.getCurrentReservation().getExpiresAt())) {
                        status = "AVAILABLE";
                    } else {
                        status = "HELD";
                    }
                } else {
                    status = "AVAILABLE";
                }
            }
            if ("AVAILABLE".equals(status) && (product.getStockQuantity() == null || product.getStockQuantity() <= 0)) {
                status = "SOLD";
            }
            result.add(BlindBoxSlotResponse.builder()
                    .slotIndex(i)
                    .status(status)
                    .build());
        }
        return result;
    }

    @Override
    public BoxReservationResponse getReservationByCode(Long userId, String reservationCode) {
        if (reservationCode == null || reservationCode.isBlank()) {
            throw new BadRequestException("Mã giữ hộp không hợp lệ!");
        }
        BoxReservation reservation = boxReservationRepository.findByReservationCode(reservationCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu giữ hộp: " + reservationCode));

        if (userId != null && !reservation.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xem phiếu giữ hộp này!");
        }

        return mapToReservationResponse(reservation);
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
                .orderCode(res.getOrderCode())
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

    @Override
    @Transactional
    public Order requestShipment(Long userId, Long addressId, List<Long> ownedItemIds) {
        ShipCabinetRequest request = ShipCabinetRequest.builder()
                .addressId(addressId)
                .ownedItemIds(ownedItemIds)
                .build();
        return requestShipment(userId, request);
    }

    @Override
    @Transactional
    public Order requestShipment(Long userId, ShipCabinetRequest request) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực tài khoản!");
        }
        if (request == null) {
            throw new BadRequestException("Thông tin yêu cầu giao hàng không hợp lệ!");
        }
        if (request.getAddressId() == null) {
            throw new BadRequestException("Vui lòng chọn địa chỉ nhận hàng!");
        }

        List<Long> itemIds = request.resolveItemIds();
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một mô hình để yêu cầu giao hàng!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // 1. Kiểm tra IDOR địa chỉ nhận hàng và snapshot địa chỉ bất biến
        UserAddress address = userAddressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ hoặc bạn không có quyền truy cập địa chỉ này!"));

        // 2. Khóa bi quan và kiểm tra từng vật phẩm (IDOR, Concurrency, Duplicate shipping)
        List<Long> distinctItemIds = itemIds.stream().distinct().toList();
        List<OwnedItem> itemsToShip = new ArrayList<>();

        for (Long itemId : distinctItemIds) {
            if (itemId == null) continue;
            OwnedItem item = ownedItemRepository.findByIdForUpdate(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mô hình với ID: " + itemId));

            // IDOR check: Vật phẩm phải thuộc về người dùng đang đăng nhập
            if (!item.getUser().getId().equals(userId)) {
                throw new BadRequestException("Bạn không có quyền yêu cầu giao hàng cho mô hình với ID: " + itemId);
            }

            // Status invariant check: Chỉ vật phẩm IN_CABINET mới được giao hàng
            if (item.getStatus() != OwnedItemStatus.IN_CABINET) {
                throw new BadRequestException("Mô hình '" + item.getBlindBoxItem().getName() + "' (ID: " + itemId + ") không ở trong tủ đồ hoặc đã được yêu cầu giao hàng trước đó!");
            }

            itemsToShip.add(item);
        }

        if (itemsToShip.isEmpty()) {
            throw new BadRequestException("Không có mô hình hợp lệ nào được chọn để giao hàng!");
        }

        // 3. Tạo mã đơn hàng giao vận
        String orderCode = "ORD" + System.currentTimeMillis() + String.format("%04d", secureRandom.nextInt(10000));

        // 4. Tạo Order với snapshot địa chỉ bất biến và POP NOW delivery semantics
        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .provinceCity(address.getProvinceCity())
                .district(address.getDistrict())
                .ward(address.getWard() != null ? address.getWard() : "")
                .detailedAddress(address.getDetailedAddress())
                .deliveryMethod("POP_NOW_SHIP")
                .subtotalAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .pointsEarned(0)
                .pointsUsed(0)
                .pointsDiscount(BigDecimal.ZERO)
                .status("PROCESSING")
                .paymentMethod("POP_NOW")
                .paidAt(LocalDateTime.now())
                .note("Đơn giao hàng POP NOW Virtual Cabinet (" + itemsToShip.size() + " mô hình)")
                .build();

        order = orderRepository.save(order);

        // 5. Tạo OrderItem cho từng mô hình và chuyển trạng thái OwnedItem sang REQUESTED_SHIPPING
        List<OrderItem> orderItems = new ArrayList<>();
        for (OwnedItem item : itemsToShip) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(item.getProduct())
                    .quantity(1)
                    .unitPrice(BigDecimal.ZERO)
                    .totalPrice(BigDecimal.ZERO)
                    .purchaseType("POP_NOW")
                    .build();
            orderItems.add(orderItem);

            item.setStatus(OwnedItemStatus.REQUESTED_SHIPPING);
            ownedItemRepository.save(item);
        }

        orderItemRepository.saveAll(orderItems);

        log.info("Yêu cầu giao hàng Virtual Cabinet thành công: User ID={}, OrderCode={}, ItemsCount={}",
                userId, orderCode, itemsToShip.size());

        return order;
    }
}
