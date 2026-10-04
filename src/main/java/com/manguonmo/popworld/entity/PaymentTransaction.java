package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thực thể lưu vết từng giao dịch thanh toán ngân hàng (SePay / VietQR) gắn với đơn hàng.
 * Phục vụ đối soát chuyển khoản đa lần (cumulative partial payment),
 * ghi nhận chuyển thừa (overpayment) và chống trùng lặp (Idempotency).
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_tx_code", columnList = "transaction_code", unique = true),
    @Index(name = "idx_tx_order_id", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Builder.Default
    @Column(name = "gateway", nullable = false, length = 30)
    private String gateway = "SEPAY";

    @Column(name = "transaction_code", nullable = false, unique = true, length = 100)
    private String transactionCode;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "accumulated_after", precision = 12, scale = 2)
    private BigDecimal accumulatedAfter;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "content", length = 255)
    private String content;

    // PARTIAL, FULL, OVERPAID, EXPIRED_ORDER
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;
}
