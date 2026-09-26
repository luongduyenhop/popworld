package com.manguonmo.popworld.scheduler;

import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.OrderItemRepository;

import com.manguonmo.popworld.repository.OrderRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.service.CouponService;
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
    private final com.manguonmo.popworld.repository.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public OrderCleanupScheduler(OrderRepository orderRepository,
                                 ProductRepository productRepository,
                                 OrderItemRepository orderItemRepository,
                                 CouponService couponService,
                                 com.manguonmo.popworld.repository.UserRepository userRepository) {

        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.couponService = couponService;
        this.userRepository = userRepository;
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
            log.info("⏰ Phát hiện {} đơn hàng quá hạn 15 phút chưa thanh toán. Đang tiến hành cập nhật trạng thái EXPIRED...", expiredOrders.size());

            for (Order order : expiredOrders) {
                order.setStatus("EXPIRED");
                order.setNote("Đơn hàng tự động hết hạn do quá hạn 15 phút chưa hoàn tất thanh toán.");
                List<OrderItem> orderList = orderItemRepository.findByOrderId(order.getId());
                if (order.getCoupon() != null) {
                    Long userId = order.getUser() != null ? order.getUser().getId() : null;
                    couponService.releaseCoupon(order.getCoupon().getId(), userId);
                }
                if (order.getPointsUsed() != null && order.getPointsUsed() > 0 && order.getUser() != null) {
                    User orderUser = order.getUser();
                    orderUser.setRewardPoints((orderUser.getRewardPoints() != null ? orderUser.getRewardPoints() : 0) + order.getPointsUsed());
                    if (userRepository != null) {
                        userRepository.save(orderUser);
                    }
                }
                for (OrderItem item : orderList) {
                    if (item.getProduct() != null && item.getProduct().getId() != null) {
                        productRepository.addStock(item.getProduct().getId(), item.getRequiredStockBoxes());
                    }
                }
            }


            orderRepository.saveAll(expiredOrders);
            log.info("✅ Đã tự động chuyển trạng thái EXPIRED và hoàn trả tồn kho thành công cho {} đơn hàng quá hạn.", expiredOrders.size());
        }
    }
}

