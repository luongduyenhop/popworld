package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_created_at", columnList = "created_at"),
    @Index(name = "idx_orders_status_expires_at", columnList = "status, expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 30)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "province_city", nullable = false, length = 100)
    private String provinceCity;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "detailed_address", nullable = false, length = 255)
    private String detailedAddress;

    @Builder.Default
    @Column(name = "delivery_method", length = 50)
    private String deliveryMethod = "Standard";

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Builder.Default
    @Column(name = "shipping_fee", precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    public BigDecimal getPaidAmount() {
        return paidAmount != null ? paidAmount : BigDecimal.ZERO;
    }

    @Builder.Default
    @Column(name = "points_earned")
    private Integer pointsEarned = 0;

    @Builder.Default
    @Column(name = "points_used")
    private Integer pointsUsed = 0;

    @Builder.Default
    @Column(name = "points_discount", precision = 12, scale = 2)
    private BigDecimal pointsDiscount = BigDecimal.ZERO;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    // TO_PAY, PROCESSING, SHIPPING, DELIVERED, CANCELLED, EXPIRED
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    // COD, CREDIT_CARD, VNPAY
    @Builder.Default
    @Column(name = "payment_method", length = 30)
    private String paymentMethod = "COD";

    // Thời điểm hết hạn giữ đơn hàng (15 phút đếm ngược)
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Thời điểm thanh toán
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Đơn vị vận chuyển (GHN, GHTK, Viettel Post, Shopee Xpress...)
    @Column(name = "carrier", length = 50)
    private String carrier;

    // Mã vận đơn (Tracking Code / Waybill)
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    // Thời điểm hoàn tất đóng gói kiểm hàng
    @Column(name = "packed_at")
    private LocalDateTime packedAt;

    // Thời điểm bàn giao cho đơn vị vận chuyển
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    // Thời điểm giao hàng thành công đến khách
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "note", length = 255)
    private String note;
}