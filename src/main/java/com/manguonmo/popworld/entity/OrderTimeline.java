package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Thực thể ghi nhận Lịch sử Hành trình Đơn Hàng (Order Timeline / Event Audit).
 * Theo dõi chi tiết từng bước chuyển trạng thái, người thực hiện, thời điểm và ghi chú vận hành.
 */
@Entity
@Table(name = "order_timelines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTimeline extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Trạng thái ban đầu trước khi chuyển
    @Column(name = "from_status", length = 30)
    private String fromStatus;

    // Trạng thái mới sau khi chuyển
    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    // Tên hành động: Đặt hàng thành công, Xác nhận thanh toán, Đóng gói niêm phong, Bàn giao shipper, Giao thành công, Hủy đơn
    @Column(name = "action", nullable = false, length = 150)
    private String action;

    // Tác nhân thực hiện: "Hệ thống", "Khách hàng", hoặc username/email của Admin/Staff
    @Column(name = "actor", length = 100)
    private String actor;

    // Ghi chú chi tiết (ví dụ: lý do hủy, thông tin kiện hàng, đơn vị vận chuyển)
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
