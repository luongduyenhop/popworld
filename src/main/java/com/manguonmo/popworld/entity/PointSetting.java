package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "point_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tỷ lệ tích điểm: Số tiền (VNĐ) chi tiêu để nhận 1 điểm thưởng.
     * Mặc định: 10.000 VNĐ = 1 điểm.
     */
    @Builder.Default
    @Column(name = "earn_rate", nullable = false)
    private Integer earnRate = 10000;

    /**
     * Tỷ lệ tiêu điểm: Giá trị quy đổi của 1 điểm ra tiền mặt (VNĐ) khi thanh toán.
     * Mặc định: 1 điểm = 100 VNĐ.
     */
    @Builder.Default
    @Column(name = "redeem_rate", nullable = false)
    private Integer redeemRate = 100;

    /**
     * Phần trăm tối đa giá trị tiền hàng được giảm trừ bằng điểm (0 - 100%).
     * Mặc định: 20%.
     */
    @Builder.Default
    @Column(name = "max_redeem_percent", nullable = false)
    private Integer maxRedeemPercent = 20;

    /**
     * Trạng thái kích hoạt chương trình điểm thưởng (true = bật, false = tạm tắt).
     */
    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}
