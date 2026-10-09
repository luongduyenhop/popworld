package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Thực thể ghi nhận Sổ Nhật Ký Biến Động Kho (Inventory Audit Log / Stock Movement).
 * Theo dõi chi tiết mọi biến động nhập, xuất, kiểm kê, đơn hàng cùng thời điểm và tác nhân.
 */
@Entity
@Table(name = "inventory_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Loại biến động kho:
     * - RESTOCK: Nhập kho thêm theo đợt
     * - ADJUST: Kiểm kê điều chỉnh thực tế
     * - ORDER_DEDUCT: Xuất kho khi khách đặt đơn hàng
     * - ORDER_CANCEL_REFUND: Hoàn kho khi hủy đơn hàng
     */
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    // Số lượng thay đổi (dương là tăng, âm là giảm. Vd: +10, -2, -5)
    @Column(name = "quantity_changed", nullable = false)
    private Integer quantityChanged;

    // Số lượng tồn trước khi biến động
    @Column(name = "old_stock", nullable = false)
    private Integer oldStock;

    // Số lượng tồn sau khi biến động
    @Column(name = "new_stock", nullable = false)
    private Integer newStock;

    // Lý do hoặc ghi chú lô hàng / mã đơn hàng
    @Column(name = "reason", length = 500)
    private String reason;

    // Người/Hệ thống thực hiện: "Admin", "Hệ thống (Order)", username
    @Column(name = "actor", length = 100)
    private String actor;
}
