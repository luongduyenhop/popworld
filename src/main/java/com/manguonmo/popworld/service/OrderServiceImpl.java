package com.manguonmo.popworld.service;

import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CouponRepository couponRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    // [CHÚ THÍCH]: Constructor Injection chuẩn Spring Boot (dọn dẹp các tham số thừa)
    public OrderServiceImpl(OrderRepository orderRepository,
                            CartItemRepository cartItemRepository,
                            CouponRepository couponRepository,
                            OrderItemRepository orderItemRepository,
                            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.couponRepository = couponRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
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

        // =========================================================================
        // BƯỚC 1: Tìm thông tin người dùng trong CSDL bằng UserRepository
        // [CHÚ THÍCH]: Phải gọi userRepository.findById(), KHÔNG gọi orderRepository.existsById().
        // Nếu không tìm thấy User, ném ra IllegalArgumentException thay vì return null
        // để tầng Controller biết chính xác nguyên nhân lỗi.
        // =========================================================================
        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId)
        );

        // =========================================================================
        // BƯỚC 2: Lấy danh sách các món đồ trong giỏ hàng của User
        // [CHÚ THÍCH]: Nếu giỏ hàng rỗng, ném Exception rõ ràng thay vì System.out.println
        // để tránh lỗi NullPointerException ở tầng Controller/UI.
        // =========================================================================
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng của bạn đang trống, không thể đặt hàng!");
        }

        // =========================================================================
        // BƯỚC 3: Tính tiền hàng (subtotal)
        // [CHÚ THÍCH QUAN TRỌNG VỀ JAVA STRING]:
        // 1. Tuyệt đối KHÔNG dùng toán tử '==' để so sánh chuỗi (cart.getPurchaseType() == "Single").
        //    '==' so sánh địa chỉ ô nhớ, không so sánh nội dung. Bắt buộc dùng .equalsIgnoreCase().
        // 2. Giá trị quy cách chuẩn trong CSDL là "SINGLE_BOX" và "WHOLE_SET".
        // 3. Getter giá hộp lẻ trong Product.java là getSinglePrice().
        // =========================================================================
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cart : cartItems) {
            BigDecimal unitPrice;
            if ("SINGLE_BOX".equalsIgnoreCase(cart.getPurchaseType())) {
                unitPrice = cart.getProduct().getSinglePrice();
            } else {
                unitPrice = cart.getProduct().getWholeSetPrice();
            }
            // Cộng dồn tiền: subtotal = subtotal.add(...) vì BigDecimal là Immutable (bất biến)
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        // =========================================================================
        // BƯỚC 4: Tính phí vận chuyển (shippingFee)
        // [CHÚ THÍCH]: Chính sách PopWorld: Đơn hàng từ 500.000đ trở lên được FREESHIP (0đ).
        // Đơn hàng dưới 500.000đ áp dụng phí tiêu chuẩn 30.000đ.
        // Phí ship phải được tách riêng vào biến shippingFee để lưu vào cột shipping_fee của Order.
        // =========================================================================
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (subtotal.compareTo(BigDecimal.valueOf(500000)) < 0) {
            shippingFee = BigDecimal.valueOf(30000);
        }

        // =========================================================================
        // BƯỚC 5: Kiểm tra và áp dụng mã giảm giá (Coupon)
        // [CHÚ THÍCH]: Kiểm tra xem mã coupon có tồn tại và còn active không,
        // đã hết hạn sử dụng chưa (endDate), và đơn hàng có đạt giá trị tối thiểu không (minOrderAmount).
        // =========================================================================
        BigDecimal discount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;

        if (couponCode != null && !couponCode.trim().isEmpty()) {
            Optional<Coupon> couponOpt = couponRepository.findByCodeAndActiveTrue(couponCode.trim());
            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();
                // Kiểm tra hạn sử dụng
                boolean isExpired = coupon.getEndDate() != null && LocalDate.now().isAfter(coupon.getEndDate());
                // Kiểm tra giá trị đơn tối thiểu
                boolean isEligible = coupon.getMinOrderAmount() == null || subtotal.compareTo(coupon.getMinOrderAmount()) >= 0;

                if (!isExpired && isEligible) {
                    // Tính số tiền giảm giá
                    if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())) {
                        // Nếu discountValue > 1 (ví dụ 10%), chia cho 100
                        BigDecimal rate = coupon.getDiscountValue().compareTo(BigDecimal.ONE) > 0
                                ? coupon.getDiscountValue().divide(BigDecimal.valueOf(100))
                                : coupon.getDiscountValue();
                        discount = subtotal.multiply(rate);
                    } else {
                        // Giảm giá cố định theo số tiền (FIXED)
                        discount = coupon.getDiscountValue();
                    }

                    // Giới hạn số tiền giảm tối đa (nếu có maxDiscountAmount)
                    if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                        discount = coupon.getMaxDiscountAmount();
                    }

                    appliedCoupon = coupon;
                }
            }
        }

        // =========================================================================
        // BƯỚC 6: Tính tổng tiền thanh toán cuối cùng (totalAmount)
        // [CHÚ THÍCH]: Tổng thanh toán = Tiền hàng (subtotal) + Tiền ship (shippingFee) - Giảm giá (discount)
        // Đảm bảo tổng tiền không bao giờ âm (< 0).
        // =========================================================================
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // =========================================================================
        // BƯỚC 7: Sinh mã đơn hàng duy nhất (orderCode)
        // [CHÚ THÍCH]: Tiền tố "PW-" kết hợp dấu thời gian hiện tại
        // =========================================================================
        String orderCode = "PW-" + System.currentTimeMillis();

        // =========================================================================
        // BƯỚC 8: Khởi tạo và Lưu Order vào CSDL
        // [CHÚ THÍCH CÁC TRƯỜNG BẮT BUỘC NOT NULL]:
        // 1. user: Bắt buộc truyền đối tượng user (khóa ngoại user_id)
        // 2. status: Bắt buộc set "TO_PAY" (chờ thanh toán)
        // 3. expiresAt: Đếm ngược 15 phút (LocalDateTime.now().plusMinutes(15))
        // 4. shippingFee: Lưu riêng phí ship đã tính ở Bước 4
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
        List<OrderItem> orderItems = cartItems.stream().map(
                cartItem -> {
                    BigDecimal unitPrice = "SINGLE_BOX".equalsIgnoreCase(cartItem.getPurchaseType())
                            ? cartItem.getProduct().getSinglePrice()
                            : cartItem.getProduct().getWholeSetPrice();

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
        // BƯỚC 10: Xóa các CartItem đã đặt khỏi giỏ hàng
        // [CHÚ THÍCH AN TOÀN DỮ LIỆU]:
        // Phải gọi cartItemRepository.deleteAll(cartItems) -> Chỉ xóa các món của đơn hàng này!
        // Tuyệt đối KHÔNG gọi cartItemRepository.deleteAll() không tham số vì lệnh đó
        // sẽ XÓA SẠCH giỏ hàng của TẤT CẢ mọi người dùng khác trên toàn hệ thống!
        // =========================================================================
        cartItemRepository.deleteAll(cartItems);

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
}
