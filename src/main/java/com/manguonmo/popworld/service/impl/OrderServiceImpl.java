package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.response.OrderItemResponse;
import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.dto.response.OrderStatusCountResponse;
import com.manguonmo.popworld.dto.response.OrderTimelineResponse;
import com.manguonmo.popworld.mapper.OrderMapper;
import com.manguonmo.popworld.service.CouponService;
import com.manguonmo.popworld.service.OrderService;
import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.OutOfStockException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CouponService couponService;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final BoxReservationRepository boxReservationRepository;

    private final OrderTimelineRepository orderTimelineRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manguonmo.popworld.service.InventoryService inventoryService;

    private static final java.util.concurrent.atomic.AtomicInteger ORDER_SEQ =
            new java.util.concurrent.atomic.AtomicInteger(100000);

    public String generateOrderCode() {
        int seq = ORDER_SEQ.updateAndGet(val -> (val >= 999999) ? 100000 : val + 1);
        return "PW-" + System.currentTimeMillis() + seq;
    }

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartItemRepository cartItemRepository,
                            CouponService couponService,
                            OrderItemRepository orderItemRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            OrderMapper orderMapper,
                            BoxReservationRepository boxReservationRepository,
                            OrderTimelineRepository orderTimelineRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.couponService = couponService;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
        this.boxReservationRepository = boxReservationRepository;
        this.orderTimelineRepository = orderTimelineRepository;
    }

    /**
     * Tạo đơn hàng mới từ các món đồ trong giỏ hàng
     * [CHÚ THÍCH @Transactional]: Đảm bảo tính nguyên tử (Atomicity).
     * Toàn bộ các thao tác: Lưu Order, Lưu danh sách OrderItem, Xóa CartItem
     * phải cùng thành công. Nếu có bất kỳ lỗi nào xảy ra giữa chừng, hệ thống
     * sẽ tự động ROLLBACK toàn bộ về trạng thái ban đầu, không để lại đơn hàng rác.
     */
    @Transactional
    @Override
    public Order createOrder(Long userId, String recipientName, String recipientPhone,
                             String provinceCity, String district, String ward,
                             String detailedAddress, String paymentMethod, String couponCode) {
        return createOrder(userId, recipientName, recipientPhone, provinceCity, district, ward, detailedAddress, paymentMethod, couponCode, 0);
    }

    @Transactional
    @Override
    public Order createOrder(Long userId, String recipientName, String recipientPhone,
                             String provinceCity, String district, String ward,
                             String detailedAddress, String paymentMethod, String couponCode,
                             Integer pointsToUse) {

        // =========================================================================
        // BƯỚC 1: Tìm thông tin người dùng trong CSDL bằng UserRepository
        // =========================================================================
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId)
        );

        // =========================================================================
        // BƯỚC 2: Lấy danh sách các món đồ ĐƯỢC CHỌN trong giỏ hàng của User
        // =========================================================================
        List<CartItem> selectedCartItems = cartItemRepository.findByUserIdAndIsSelectedTrue(userId);
        if (selectedCartItems.isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang trống hoặc chưa chọn sản phẩm nào để đặt hàng!");
        }

        // =========================================================================
        // BƯỚC 3: Tính tiền hàng (subtotal) & Trừ tồn kho nguyên tử
        // [ANTI-DEADLOCK]: Sắp xếp deterministic theo product.id ASC trước khi lock/update
        // Tránh tình trạng Thread 1 giữ lock Product A đòi B, Thread 2 giữ B đòi A gây Deadlock MySQL!
        // =========================================================================
        List<CartItem> orderedCartItems = new java.util.ArrayList<>(selectedCartItems);
        orderedCartItems.sort(java.util.Comparator.comparing(item -> item.getProduct().getId()));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cart : orderedCartItems) {
            BigDecimal unitPrice;
            if ("SINGLE_BOX".equalsIgnoreCase(cart.getPurchaseType())) {
                unitPrice = cart.getProduct().getSinglePrice();
            } else {
                unitPrice = cart.getProduct().getWholeSetPrice() != null
                        ? cart.getProduct().getWholeSetPrice()
                        : cart.getProduct().getSinglePrice();
            }
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(cart.getQuantity())));
            int updateRows = productRepository.updateStock(cart.getProduct().getId(), cart.getRequiredStockBoxes());

            if (updateRows == 0) {
                throw new OutOfStockException("Sản phẩm " + cart.getProduct().getName() + " đã hết hàng hoặc không đủ số lượng tồn kho!");
            }
        }

        // =========================================================================
        // BƯỚC 4: Tính phí vận chuyển (shippingFee)
        // =========================================================================
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (subtotal.compareTo(BigDecimal.valueOf(500000)) < 0) {
            shippingFee = BigDecimal.valueOf(30000);
        }

        // =========================================================================
        // BƯỚC 5: Kiểm tra và áp dụng mã giảm giá (Coupon)
        // =========================================================================
        BigDecimal discount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;

        if (couponCode != null && !couponCode.trim().isEmpty()) {
            CouponDiscountResponse couponDiscountResponse = couponService.calculateDiscount(couponCode, userId, subtotal);
            discount = couponDiscountResponse.getDiscountAmount();
            appliedCoupon = couponService.applyCoupon(couponCode, userId, subtotal);
        }

        // =========================================================================
        // BƯỚC 6: Xử lý điểm thưởng (Reward Points: 10.000 VND = 1 point, 1 point = 100 VND, max 20% subtotal)
        // =========================================================================
        int pointsEarned = subtotal.divideToIntegralValue(BigDecimal.valueOf(10000)).intValue();
        int finalPointsUsed = 0;
        BigDecimal pointsDiscount = BigDecimal.ZERO;

        if (pointsToUse != null && pointsToUse > 0) {
            int currentPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
            if (pointsToUse > currentPoints) {
                throw new BadRequestException("Số điểm sử dụng (" + pointsToUse + ") vượt quá số điểm hiện có (" + currentPoints + ") của bạn!");
            }

            pointsDiscount = BigDecimal.valueOf(pointsToUse * 100L);
            BigDecimal maxAllowedDiscount = subtotal.multiply(new BigDecimal("0.20"));
            if (pointsDiscount.compareTo(maxAllowedDiscount) > 0) {
                throw new BadRequestException("Điểm thưởng chỉ được giảm tối đa 20% giá trị tiền hàng (tối đa " + maxAllowedDiscount.intValue() + " đ)!");
            }

            finalPointsUsed = pointsToUse;
            user.setRewardPoints(currentPoints - finalPointsUsed);
            userRepository.save(user);
        }

        // =========================================================================
        // BƯỚC 7: Tính tổng tiền thanh toán cuối cùng (totalAmount)
        // =========================================================================
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discount).subtract(pointsDiscount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // =========================================================================
        // BƯỚC 8: Sinh mã đơn hàng duy nhất (orderCode)
        // =========================================================================
        String orderCode = generateOrderCode();

        // =========================================================================
        // BƯỚC 9: Khởi tạo và Lưu Order vào CSDL
        // =========================================================================
        Order newOrder = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .provinceCity(provinceCity)
                .district(district)
                .ward(ward)
                .detailedAddress(detailedAddress)
                .subtotalAmount(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discount)
                .pointsEarned(pointsEarned)
                .pointsUsed(finalPointsUsed)
                .pointsDiscount(pointsDiscount)
                .totalAmount(totalAmount)
                .paymentMethod(paymentMethod != null ? paymentMethod : "COD")
                .status("TO_PAY")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .coupon(appliedCoupon)
                .build();

        Order savedOrder = orderRepository.save(newOrder);


        // =========================================================================
        // BƯỚC 9: Tạo danh sách OrderItem và Lưu vào CSDL
        // [CHÚ THÍCH CỰC KỲ QUAN TRỌNG VỀ QUAN HỆ KHÓA NGOẠI]:
        // 1. orderItem.setOrder(savedOrder): Bắt buộc gán Order cha, nếu không MySQL sẽ lỗi
        //    "Column 'order_id' cannot be null"!
        // 2. orderItem.setTotalPrice(...): Là giá của RIÊNG món này (unitPrice * quantity),
        //    tuyệt đối KHÔNG gán totalAmount của cả đơn hàng vào từng món!
        // =========================================================================
        List<OrderItem> orderItems = selectedCartItems.stream().map(
                cartItem -> {
                    BigDecimal unitPrice = "SINGLE_BOX".equalsIgnoreCase(cartItem.getPurchaseType())
                            ? cartItem.getProduct().getSinglePrice()
                            : (cartItem.getProduct().getWholeSetPrice() != null ? cartItem.getProduct().getWholeSetPrice() : cartItem.getProduct().getSinglePrice());

                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(savedOrder); // Gắn quan hệ với Order vừa tạo
                    orderItem.setProduct(cartItem.getProduct());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setPurchaseType(cartItem.getPurchaseType());
                    orderItem.setUnitPrice(unitPrice);
                    orderItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                    return orderItem;
                }
        ).toList();

        orderItemRepository.saveAll(orderItems);    

        // Ghi log xuất kho vào Sổ Nhật Ký Biến Động Kho (Audit Log)
        if (inventoryService != null) {
            for (CartItem cartItem : selectedCartItems) {
                try {
                    Product prod = cartItem.getProduct();
                    if (prod != null && prod.getId() != null) {
                        int reqBoxes = cartItem.getRequiredStockBoxes();
                        int curStock = prod.getStockQuantity() != null ? prod.getStockQuantity() : 0;
                        inventoryService.recordStockLog(prod, "ORDER_DEDUCT", -reqBoxes,
                                curStock, Math.max(0, curStock - reqBoxes),
                                "Xuất bán đơn hàng #" + savedOrder.getOrderCode(),
                                "Khách hàng (" + user.getEmail() + ")");
                    }
                } catch (Exception ex) {
                    log.warn("Không thể ghi log xuất kho cho đơn {}: {}", savedOrder.getOrderCode(), ex.getMessage());
                }
            }
        }

        // =========================================================================
        // BƯỚC 10: Xóa CHỈ các CartItem ĐÃ ĐẶT HÀNG (được chọn) khỏi giỏ hàng
        // [CHÚ THÍCH AN TOÀN DỮ LIỆU]:
        // Phải gọi cartItemRepository.deleteAll(selectedCartItems) -> Chỉ xóa các món đã chọn của đơn này!
        // Các món không được chọn vẫn được giữ lại an toàn trong giỏ của người dùng.
        // =========================================================================
        cartItemRepository.deleteAll(selectedCartItems);

        // =========================================================================
        // BƯỚC 11: Trả về đối tượng Order đã lưu thành công
        // =========================================================================
        recordTimeline(savedOrder, null, "TO_PAY", "Đặt hàng thành công", 
                (user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail()), 
                "Phương thức thanh toán: " + savedOrder.getPaymentMethod() + " | Đơn giá trị: " + savedOrder.getTotalAmount() + " đ");
        return savedOrder;
    }

    @Override
    public Order getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode).orElse(null);
    }

    @Override
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Order cancelOrder(Long userId, String orderCode, String reason) {
        Order order = orderRepository.findByOrderCode(orderCode).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy đơn hàng này")
        );

        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền hủy đơn hàng này!");
        }

        String orderStatus = order.getStatus();
        if ("CANCELLED".equalsIgnoreCase(orderStatus)) {
            throw new BadRequestException("Đơn hàng này đã bị hủy từ trước!");
        }
        if ("EXPIRED".equalsIgnoreCase(orderStatus)) {
            throw new BadRequestException("Đơn hàng này đã hết hạn thanh toán từ trước!");
        }
        if (!"TO_PAY".equalsIgnoreCase(orderStatus)) {
            throw new BadRequestException("Khách hàng chỉ có thể hủy đơn hàng ở trạng thái Chờ thanh toán (TO_PAY). Trạng thái hiện tại: " + orderStatus);
        }

        List<OrderItem> itemList = orderItemRepository.findByOrderId(order.getId());
        boolean isPopNowOrder = "POP_NOW_CABINET".equalsIgnoreCase(order.getDeliveryMethod());
        if (!itemList.isEmpty()) {
            for (OrderItem item : itemList) {
                boolean isPopNowItem = "POP_NOW".equalsIgnoreCase(item.getPurchaseType());
                if (!isPopNowOrder && !isPopNowItem && item.getProduct() != null && item.getProduct().getId() != null) {
                    productRepository.addStock(item.getProduct().getId(), item.getRequiredStockBoxes());
                    if (inventoryService != null) {
                        try {
                            Product prod = item.getProduct();
                            int curStock = prod.getStockQuantity() != null ? prod.getStockQuantity() : 0;
                            inventoryService.recordStockLog(prod, "ORDER_CANCEL_REFUND", item.getRequiredStockBoxes(),
                                    curStock, curStock + item.getRequiredStockBoxes(),
                                    "Hoàn kho do khách hủy đơn #" + order.getOrderCode(), "Khách hàng");
                        } catch (Exception ex) {
                            log.warn("Không thể ghi log hoàn kho đơn {}: {}", order.getOrderCode(), ex.getMessage());
                        }
                    }
                }
            }
        }
        if (isPopNowOrder && boxReservationRepository != null) {
            boxReservationRepository.findByOrderCode(order.getOrderCode()).ifPresent(res -> {
                if (res.getStatus() == ReservationStatus.RESERVED) {
                    res.setStatus(ReservationStatus.CANCELLED);
                    boxReservationRepository.save(res);
                    if (res.getSlot() != null && res.getSlot().getStatus() == SlotStatus.HELD) {
                        BlindBoxSlot slot = res.getSlot();
                        slot.setStatus(SlotStatus.AVAILABLE);
                        slot.setCurrentReservation(null);
                    }
                    if (res.getProduct() != null && res.getProduct().getId() != null) {
                        productRepository.addStock(res.getProduct().getId(), 1);
                    }
                }
            });
        }
        if (order.getCoupon() != null) {
            couponService.releaseCoupon(order.getCoupon().getId(), userId);
        }
        if (order.getPointsUsed() != null && order.getPointsUsed() > 0 && order.getUser() != null) {
            User orderUser = order.getUser();
            orderUser.setRewardPoints((orderUser.getRewardPoints() != null ? orderUser.getRewardPoints() : 0) + order.getPointsUsed());
            userRepository.save(orderUser);
        }
        order.setStatus("CANCELLED");
        String cancelNote = (reason != null && !reason.trim().isEmpty())
                ? "Khách hàng hủy đơn: " + reason.trim()
                : "Khách hàng chủ động hủy đơn.";
        order.setNote(cancelNote);
        Order savedOrder = orderRepository.save(order);
        recordTimeline(savedOrder, "TO_PAY", "CANCELLED", "Khách hàng hủy đơn hàng", 
                (order.getUser() != null && order.getUser().getFullName() != null && !order.getUser().getFullName().isBlank() ? order.getUser().getFullName() : "Khách hàng"), 
                cancelNote);
        return savedOrder;

    }

    @Override
    @Transactional
    public Order adminCancelOrder(String orderCode, String reason) {
        return adminCancelOrder(orderCode, reason, "Quản trị viên");
    }

    @Override
    @Transactional
    public Order adminCancelOrder(String orderCode, String reason, String adminUsername) {
        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase()).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode)
        );

        String orderStatus = order.getStatus();
        if ("CANCELLED".equalsIgnoreCase(orderStatus)) {
            throw new BadRequestException("Đơn hàng này đã bị hủy từ trước!");
        }
        if ("EXPIRED".equalsIgnoreCase(orderStatus)) {
            throw new BadRequestException("Đơn hàng này đã hết hạn thanh toán từ trước!");
        }
        if (!List.of("TO_PAY", "PROCESSING", "PACKED").contains(orderStatus)) {
            throw new BadRequestException("Không thể hủy đơn hàng ở trạng thái " + orderStatus + ". Đơn hàng đang vận chuyển hoặc đã giao thành công!");
        }

        List<OrderItem> itemList = orderItemRepository.findByOrderId(order.getId());
        boolean isPopNowOrder = "POP_NOW_CABINET".equalsIgnoreCase(order.getDeliveryMethod());
        if (!itemList.isEmpty()) {
            for (OrderItem item : itemList) {
                boolean isPopNowItem = "POP_NOW".equalsIgnoreCase(item.getPurchaseType());
                if (!isPopNowOrder && !isPopNowItem && item.getProduct() != null && item.getProduct().getId() != null) {
                    productRepository.addStock(item.getProduct().getId(), item.getRequiredStockBoxes());
                    if (inventoryService != null) {
                        try {
                            Product prod = item.getProduct();
                            int curStock = prod.getStockQuantity() != null ? prod.getStockQuantity() : 0;
                            inventoryService.recordStockLog(prod, "ORDER_CANCEL_REFUND", item.getRequiredStockBoxes(),
                                    curStock, curStock + item.getRequiredStockBoxes(),
                                    "Hoàn kho do quản trị viên hủy đơn #" + order.getOrderCode(), "Admin");
                        } catch (Exception ex) {
                            log.warn("Không thể ghi log hoàn kho đơn {}: {}", order.getOrderCode(), ex.getMessage());
                        }
                    }
                }
            }
        }
        if (isPopNowOrder && boxReservationRepository != null) {
            boxReservationRepository.findByOrderCode(order.getOrderCode()).ifPresent(res -> {
                if (res.getStatus() == ReservationStatus.RESERVED) {
                    res.setStatus(ReservationStatus.CANCELLED);
                    boxReservationRepository.save(res);
                    if (res.getSlot() != null && res.getSlot().getStatus() == SlotStatus.HELD) {
                        BlindBoxSlot slot = res.getSlot();
                        slot.setStatus(SlotStatus.AVAILABLE);
                        slot.setCurrentReservation(null);
                    }
                    if (res.getProduct() != null && res.getProduct().getId() != null) {
                        productRepository.addStock(res.getProduct().getId(), 1);
                    }
                }
            });
        }
        if (order.getCoupon() != null) {
            Long userId = order.getUser() != null ? order.getUser().getId() : null;
            couponService.releaseCoupon(order.getCoupon().getId(), userId);
        }
        if (order.getPointsUsed() != null && order.getPointsUsed() > 0 && order.getUser() != null) {
            User orderUser = order.getUser();
            orderUser.setRewardPoints((orderUser.getRewardPoints() != null ? orderUser.getRewardPoints() : 0) + order.getPointsUsed());
            userRepository.save(orderUser);
        }

        order.setStatus("CANCELLED");
        String cancelNote = (reason != null && !reason.trim().isEmpty())
                ? "Admin hủy đơn: " + reason.trim()
                : "Admin chủ động hủy đơn.";
        order.setNote(cancelNote);
        Order savedOrder = orderRepository.save(order);

        recordTimeline(savedOrder, orderStatus, "CANCELLED", "Quản trị viên hủy đơn hàng", 
                adminUsername != null ? adminUsername : "Quản trị viên", 
                (reason != null && !reason.isBlank()) ? "Lý do: " + reason : "Theo quyết định của Quản trị viên");

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<OrderItem>> getOrderItemsByOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(String status) {
        List<Order> getOrders;
        if (status == null || status.trim().isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
            getOrders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            String filterStatus = status.trim().toUpperCase();
            if ("SHIPPED".equals(filterStatus)) {
                filterStatus = "SHIPPING";
            } else if ("COMPLETED".equals(filterStatus)) {
                filterStatus = "DELIVERED";
            }
            getOrders = orderRepository.findByStatusOrderByCreatedAtDesc(filterStatus);
        }
        if (getOrders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderIds = getOrders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = getOrderItemsByOrderIds(orderIds);
        return getOrders.stream().map(
                order -> orderMapper.toResponse(order, itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()))
        ).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatusCountResponse getOrderStatusCounts() {
        return OrderStatusCountResponse.builder()
                .all(orderRepository.count())
                .toPay(orderRepository.countByStatus("TO_PAY"))
                .processing(orderRepository.countByStatus("PROCESSING"))
                .packed(orderRepository.countByStatus("PACKED"))
                .shipping(orderRepository.countByStatus("SHIPPING"))
                .delivered(orderRepository.countByStatus("DELIVERED"))
                .cancelled(orderRepository.countByStatus("CANCELLED"))
                .expired(orderRepository.countByStatus("EXPIRED"))
                .build();
    }

    @Override
    @Transactional
    public Order packOrder(String orderCode, String adminUsername, String note) {
        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase()).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode)
        );

        if (!"PROCESSING".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Chỉ có thể đóng gói cho đơn hàng ở trạng thái Chờ xử lý (PROCESSING). Trạng thái hiện tại: " + order.getStatus());
        }

        order.setStatus("PACKED");
        order.setPackedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        recordTimeline(savedOrder, "PROCESSING", "PACKED", "Đã đóng gói & niêm phong Art Toy",
                adminUsername != null ? adminUsername : "Quản trị viên",
                (note != null && !note.isBlank()) ? note : "Kiện hàng đã được kiểm đếm Art Toy và dán tem niêm phong chống sốc.");

        return savedOrder;
    }

    @Override
    @Transactional
    public Order shipOrder(String orderCode) {
        return shipOrder(orderCode, "Giao Hàng Nhanh (GHN)", null, "Quản trị viên", null);
    }

    @Override
    @Transactional
    public Order shipOrder(String orderCode, String carrier, String trackingNumber, String adminUsername, String note) {
        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase()).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode)
        );
        if (!"PACKED".equalsIgnoreCase(order.getStatus()) && !"PROCESSING".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Chỉ có thể giao hàng cho đơn ở trạng thái Đã đóng gói (PACKED) hoặc Chờ xử lý (PROCESSING). Trạng thái hiện tại: " + order.getStatus());
        }

        String fromStatus = order.getStatus();
        order.setStatus("SHIPPING");
        order.setShippedAt(LocalDateTime.now());

        String assignedCarrier = (carrier != null && !carrier.isBlank()) ? carrier.trim() : "Giao Hàng Nhanh (GHN)";
        String assignedTracking = (trackingNumber != null && !trackingNumber.isBlank()) ? trackingNumber.trim() : ("VN-" + (System.currentTimeMillis() % 10000000));
        order.setCarrier(assignedCarrier);
        order.setTrackingNumber(assignedTracking);

        Order savedOrder = orderRepository.save(order);
        String actionText = "Bàn giao vận chuyển [" + assignedCarrier + " - Mã VĐ: " + assignedTracking + "]";
        recordTimeline(savedOrder, fromStatus, "SHIPPING", actionText, adminUsername != null ? adminUsername : "Quản trị viên", note);

        return savedOrder;
    }

    @Override
    @Transactional
    public Order completeOrder(String orderCode) {
        return completeOrder(orderCode, "Quản trị viên", null);
    }

    @Override
    @Transactional
    public Order completeOrder(String orderCode, String adminUsername, String note) {
        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase()).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode)
        );
        if (!"SHIPPING".equalsIgnoreCase(order.getStatus())){
            throw new BadRequestException("Chỉ có thể hoàn tất đơn hàng đang được giao (SHIPPING). Trạng thái hiện tại: " + order.getStatus());
        }

        order.setStatus("DELIVERED");
        order.setDeliveredAt(LocalDateTime.now());
        if(order.getPaidAt()==null){
            order.setPaidAt(LocalDateTime.now());
        }

        // Tích điểm thưởng cho khách hàng khi giao thành công (10.000 đ = 1 point)
        if (order.getUser() != null && order.getPointsEarned() != null && order.getPointsEarned() > 0) {
            User orderUser = order.getUser();
            orderUser.setRewardPoints((orderUser.getRewardPoints() != null ? orderUser.getRewardPoints() : 0) + order.getPointsEarned());
            userRepository.save(orderUser);
        }

        Order savedOrder = orderRepository.save(order);
        recordTimeline(savedOrder, "SHIPPING", "DELIVERED", "Giao hàng thành công đến tay khách", adminUsername != null ? adminUsername : "Quản trị viên", note);

        return savedOrder;
    }

    @Override
    @Transactional
    public void recordTimeline(Order order, String fromStatus, String toStatus, String action, String actor, String note) {
        if (orderTimelineRepository == null || order == null) {
            return;
        }
        try {
            OrderTimeline timeline = OrderTimeline.builder()
                    .order(order)
                    .fromStatus(fromStatus)
                    .toStatus(toStatus)
                    .action(action != null ? action : "Cập nhật đơn hàng")
                    .actor(actor != null && !actor.isBlank() ? actor : "Hệ thống")
                    .note(note)
                    .build();
            orderTimelineRepository.save(timeline);
        } catch (Exception e) {
            log.warn("Không thể lưu OrderTimeline cho đơn {}: {}", order.getOrderCode(), e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderTimelineResponse> getOrderTimelines(String orderCode) {
        if (orderTimelineRepository == null || orderCode == null || orderCode.isBlank()) {
            return Collections.emptyList();
        }
        return orderTimelineRepository.findByOrderOrderCodeOrderByCreatedAtAsc(orderCode.trim().toUpperCase())
                .stream()
                .map(t -> OrderTimelineResponse.builder()
                        .id(t.getId())
                        .fromStatus(t.getFromStatus())
                        .toStatus(t.getToStatus())
                        .action(t.getAction())
                        .actor(t.getActor())
                        .note(t.getNote())
                        .createdAt(t.getCreatedAt())
                        .build())
                .toList();
    }



    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> searchOrders(String keyword) {
        if(keyword == null || keyword.isBlank() ){
            return getAllOrders("ALL");
        }
        List<Order> orders = orderRepository.searchOrders(keyword.trim());
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = getOrderItemsByOrderIds(orderIds);
        return orders.stream().map(
                order -> orderMapper.toResponse(order, itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()))
        ).toList();
    }

    @Transactional
    @Override
    public Order createOrderForReservation(Long userId, String reservationCode, String paymentMethod) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực tài khoản!");
        }
        if (reservationCode == null || reservationCode.isBlank()) {
            throw new BadRequestException("Mã giữ hộp không hợp lệ!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        if (boxReservationRepository == null) {
            throw new IllegalStateException("BoxReservationRepository chưa được khởi tạo!");
        }

        BoxReservation reservation = boxReservationRepository.findByReservationCodeForUpdate(reservationCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu giữ hộp với mã: " + reservationCode));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thanh toán cho phiếu giữ hộp này!");
        }

        if (reservation.getStatus() == ReservationStatus.PURCHASED || reservation.getStatus() == ReservationStatus.UNBOXED) {
            if (reservation.getOrderCode() != null) {
                return orderRepository.findByOrderCode(reservation.getOrderCode()).orElse(null);
            }
            throw new BadRequestException("Phiếu giữ hộp đã được thanh toán!");
        }

        if (reservation.getStatus() != ReservationStatus.RESERVED || reservation.isExpired()) {
            throw new BadRequestException("Phiếu giữ hộp đã hết hạn hoặc bị hủy!");
        }

        // Tái sử dụng đơn hàng nếu đã tạo trước đó và vẫn ở trạng thái TO_PAY
        if (reservation.getOrderCode() != null) {
            Optional<Order> existingOrder = orderRepository.findByOrderCode(reservation.getOrderCode());
            if (existingOrder.isPresent() && "TO_PAY".equalsIgnoreCase(existingOrder.get().getStatus())) {
                return existingOrder.get();
            }
        }

        Product product = reservation.getProduct();
        BigDecimal price = product.getSinglePrice();
        String orderCode = generateOrderCode();

        String recipientName = user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail();
        String recipientPhone = user.getPhone() != null && !user.getPhone().isBlank() ? user.getPhone() : "0900000000";
        String province = "Hồ Chí Minh";
        String district = "Quận 1";
        String ward = "Phường Bến Nghé";
        String detailedAddress = "Tủ đồ ảo POP NOW (Virtual Cabinet)";

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .provinceCity(province)
                .district(district)
                .ward(ward)
                .detailedAddress(detailedAddress)
                .deliveryMethod("POP_NOW_CABINET")
                .subtotalAmount(price)
                .shippingFee(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(price)
                .pointsEarned(price.divideToIntegralValue(BigDecimal.valueOf(10000)).intValue())
                .pointsUsed(0)
                .pointsDiscount(BigDecimal.ZERO)
                .status("TO_PAY")
                .paymentMethod(paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "SEPAY")
                .expiresAt(reservation.getExpiresAt())
                .note("POP NOW: " + reservation.getReservationCode())
                .build();

        order = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(1)
                .unitPrice(price)
                .totalPrice(price)
                .purchaseType("POP_NOW")
                .build();

        orderItemRepository.save(orderItem);

        reservation.setOrderCode(orderCode);
        boxReservationRepository.save(reservation);

        log.info("Khởi tạo đơn hàng POP NOW thành công: orderCode={}, reservationCode={}, user={}",
                orderCode, reservationCode, user.getEmail());

        recordTimeline(order, null, "TO_PAY", "Khởi tạo đơn hàng từ POP NOW (Virtual Cabinet)", 
                (user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail()), 
                "Phiếu giữ hộp: " + reservationCode);

        return order;
    }
}
