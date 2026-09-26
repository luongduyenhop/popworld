package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.response.OrderItemResponse;
import com.manguonmo.popworld.dto.response.OrderResponse;
import com.manguonmo.popworld.dto.response.OrderStatusCountResponse;
import com.manguonmo.popworld.mapper.OrderMapper;
import com.manguonmo.popworld.service.CouponService;
import com.manguonmo.popworld.service.OrderService;
import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.OutOfStockException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CouponService couponService;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private static final java.util.concurrent.atomic.AtomicInteger ORDER_SEQ =
            new java.util.concurrent.atomic.AtomicInteger(100000);

    public String generateOrderCode() {
        int seq = ORDER_SEQ.updateAndGet(val -> (val >= 999999) ? 100000 : val + 1);
        return "PW-" + System.currentTimeMillis() + seq;
    }

    // [CHÚ THÍCH]: Constructor Injection chuẩn Spring Boot (dọn dẹp các tham số thừa)
    public OrderServiceImpl(OrderRepository orderRepository,
                            CartItemRepository cartItemRepository,
                            CouponService couponService,
                            OrderItemRepository orderItemRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.couponService = couponService;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
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
        // =========================================================================
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cart : selectedCartItems) {
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
        if (!itemList.isEmpty()) {
            for (OrderItem item : itemList) {
                productRepository.addStock(item.getProduct().getId(), item.getRequiredStockBoxes());
            }
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
        return orderRepository.save(order);

    }

    @Override
    @Transactional
    public Order adminCancelOrder(String orderCode, String reason) {
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
        if (!List.of("TO_PAY", "PROCESSING").contains(orderStatus)) {
            throw new BadRequestException("Không thể hủy đơn hàng ở trạng thái " + orderStatus + ". Đơn hàng đang vận chuyển hoặc đã giao thành công!");
        }

        List<OrderItem> itemList = orderItemRepository.findByOrderId(order.getId());
        if (!itemList.isEmpty()) {
            for (OrderItem item : itemList) {
                productRepository.addStock(item.getProduct().getId(), item.getRequiredStockBoxes());
            }
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
        return orderRepository.save(order);
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
        return getOrders.stream().map(
                order -> {
                    List<OrderItem> orderItem = orderItemRepository.findByOrderId(order.getId());
                    return orderMapper.toResponse(order,orderItem);
                }
        ).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatusCountResponse getOrderStatusCounts() {
        return OrderStatusCountResponse.builder()
                .all(orderRepository.count())
                .toPay(orderRepository.countByStatus("TO_PAY"))
                .processing(orderRepository.countByStatus("PROCESSING"))
                .shipping(orderRepository.countByStatus("SHIPPING"))
                .delivered(orderRepository.countByStatus("DELIVERED"))
                .cancelled(orderRepository.countByStatus("CANCELLED"))
                .expired(orderRepository.countByStatus("EXPIRED"))
                .build();
    }

    @Override
    @Transactional
    public Order shipOrder(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase()).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode)
        );
        if (!"PROCESSING".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Chỉ có thể giao hàng cho đơn ở trạng thái Chờ xử lý (PROCESSING). Trạng thái hiện tại: " + order.getStatus());
        }

        order.setStatus("SHIPPING");
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order completeOrder(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase()).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode)
        );
        if (!"SHIPPING".equalsIgnoreCase(order.getStatus())){
            throw new BadRequestException("Chỉ có thể hoàn tất đơn hàng đang được giao (SHIPPING). Trạng thái hiện tại: " + order.getStatus());
        }

        order.setStatus("DELIVERED");
        if(order.getPaidAt()==null){
            order.setPaidAt(LocalDateTime.now());
        }

        // Tích điểm thưởng cho khách hàng khi giao thành công (10.000 đ = 1 point)
        if (order.getUser() != null && order.getPointsEarned() != null && order.getPointsEarned() > 0) {
            User orderUser = order.getUser();
            orderUser.setRewardPoints((orderUser.getRewardPoints() != null ? orderUser.getRewardPoints() : 0) + order.getPointsEarned());
            userRepository.save(orderUser);
        }

        return orderRepository.save(order);
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
        return orders.stream().map(
                order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                    return orderMapper.toResponse(order,items);
                }
        ).toList();



    }
}
