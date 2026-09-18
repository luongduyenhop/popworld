package com.manguonmo.popworld.scheduler;

import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.repository.OrderItemRepository;
import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.service.CouponService;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled Job chạy nền tự động dọn dẹp các đơn hàng quá thời hạn 15 phút chưa thanh toán.
 * Giúp giải phóng trạng thái đơn và đảm bảo tính toàn vẹn tồn kho của hệ thống.
 */
@Component
public class OrderCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderCleanupScheduler.class);
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final CouponService couponService;
    public OrderCleanupScheduler(OrderRepository orderRepository, ProductRepository productRepository, OrderItemRepository orderItemRepository, CouponService couponService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.couponService = couponService;
    }

    /**
     * Chạy định kỳ mỗi 60 giây (60.000 ms)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findByExpiresAtBeforeAndStatus(now, "TO_PAY");

        if (!expiredOrders.isEmpty()) {
            log.info("⏰ Phát hiện {} đơn hàng quá hạn 15 phút chưa thanh toán. Đang tiến hành hủy...", expiredOrders.size());

            for (Order order : expiredOrders) {
                order.setStatus("CANCELLED");
                order.setNote("Đơn hàng tự động hủy do quá hạn 15 phút chưa hoàn tất thanh toán.");
                List<OrderItem> orderList = orderItemRepository.findByOrderId(order.getId());
                if (order.getCoupon() != null) {
                    Long userId = order.getUser() != null ? order.getUser().getId() : null;
                    couponService.releaseCoupon(order.getCoupon().getId(), userId);
                }
                for (OrderItem item : orderList){
                    Integer quantity = 0;
                    if (item.getPurchaseType().equalsIgnoreCase("SINGLE_BOX") ){
                        quantity = item.getQuantity();
                    } else {
                        quantity = item.getQuantity()*12;
                    }
                    productRepository.addStock(item.getProduct().getId(),quantity);
                }
            }

            orderRepository.saveAll(expiredOrders);
            log.info("✅ Đã tự động hủy thành công {} đơn hàng quá hạn.", expiredOrders.size());
        }
    }
}
