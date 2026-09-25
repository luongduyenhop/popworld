package com.manguonmo.popworld.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id", "purchase_type"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "purchase_type", nullable = false, length = 20)
    private String purchaseType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Builder.Default
    @Column(name = "is_selected")
    private Boolean isSelected = true;

    public static final int BOXES_PER_WHOLE_SET = 12;

    public int getRequiredStockBoxes() {
        int qty = (this.quantity != null) ? this.quantity : 0;
        return "WHOLE_SET".equalsIgnoreCase(this.purchaseType) ? qty * BOXES_PER_WHOLE_SET : qty;
    }
}