package com.manguonmo.popworld.controller;

import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.OrderItemRepository;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Controller điều phối luồng Thanh toán (Checkout), VietQR SePay, và Quản lý Đơn hàng.
 */
@Controller
public class CheckoutWebController {

    private final OrderService orderService;
    private final CartService cartService;
    private final OrderItemRepository orderItemRepository;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;

    @Value("${sepay.bank-code:MBBank}")
    private String sepayBankCode;

    @Value("${sepay.account-number:0355416208}")
    private String sepayAccountNumber;

    @Value("${sepay.account-name:POPWORLD OFFICIAL STORE}")
    private String sepayAccountName;

    public CheckoutWebController(OrderService orderService,
                                 CartService cartService,
                                 OrderItemRepository orderItemRepository,
                                 CategoryService categoryService,
                                 CharacterIpService characterIpService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.orderItemRepository = orderItemRepository;
        this.categoryService = categoryService;
        this.characterIpService = characterIpService;
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
    }

    /**
     * Hiển thị trang điền thông tin đặt hàng (Checkout)
     */
    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, RedirectAttributes redirectAttributes) {
        addCommonAttributes(model);
        User user = cartService.getDefaultUser();
        List<CartItem> cartItems = cartService.getCartItems(user.getId());

        if (cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi thanh toán!");
            return "redirect:/cart";
        }

        BigDecimal subtotal = cartService.calculateSelectedTotal(user.getId());
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            // Nếu chưa tick chọn món nào, mặc định tính toàn bộ món trong giỏ
            subtotal = cartItems.stream()
                    .map(item -> {
                        BigDecimal price = "SINGLE_BOX".equalsIgnoreCase(item.getPurchaseType())
                                ? item.getProduct().getSinglePrice()
                                : item.getProduct().getWholeSetPrice();
                        return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal shippingFee = subtotal.compareTo(BigDecimal.valueOf(500000)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(30000);
        BigDecimal totalAmount = subtotal.add(shippingFee);

        model.addAttribute("user", user);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("totalAmount", totalAmount);

        return "checkout";
    }

    /**
     * Tiếp nhận form đặt hàng
     */
    @PostMapping("/checkout/place-order")
    public String placeOrder(@RequestParam String recipientName,
                             @RequestParam String recipientPhone,
                             @RequestParam String provinceCity,
                             @RequestParam String district,
                             @RequestParam(required = false, defaultValue = "") String ward,
                             @RequestParam String detailedAddress,
                             @RequestParam(defaultValue = "COD") String paymentMethod,
                             @RequestParam(required = false) String couponCode,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = cartService.getDefaultUser();
            Order order = orderService.createOrder(
                    user.getId(),
                    recipientName,
                    recipientPhone,
                    provinceCity,
                    district,
                    ward,
                    detailedAddress,
                    paymentMethod,
                    couponCode
            );

            if ("SEPAY".equalsIgnoreCase(paymentMethod)) {
                return "redirect:/checkout/payment/" + order.getOrderCode();
            } else {
                return "redirect:/checkout/success/" + order.getOrderCode();
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đặt hàng thất bại: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    /**
     * Màn hình thanh toán chuyển khoản qua VietQR SePay động
     */
    @GetMapping("/checkout/payment/{orderCode}")
    public String showPaymentQrPage(@PathVariable String orderCode, Model model) {
        addCommonAttributes(model);
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            return "redirect:/cart";
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        // Tạo URL QR VietQR động qua SePay:
        // Cú pháp: https://qr.sepay.vn/img?acc={STK}&bank={BANK}&amount={AMOUNT}&des={DES}
        String encodedDes = URLEncoder.encode(order.getOrderCode(), StandardCharsets.UTF_8);
        String qrUrl = String.format(
                "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s",
                sepayAccountNumber,
                sepayBankCode,
                order.getTotalAmount().longValue(),
                encodedDes
        );

        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("bankCode", sepayBankCode);
        model.addAttribute("accountNumber", sepayAccountNumber);
        model.addAttribute("accountName", sepayAccountName);

        return "payment-sepay";
    }

    /**
     * Màn hình thông báo đặt hàng thành công
     */
    @GetMapping("/checkout/success/{orderCode}")
    public String showOrderSuccessPage(@PathVariable String orderCode, Model model) {
        addCommonAttributes(model);
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            return "redirect:/";
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        model.addAttribute("order", order);
        model.addAttribute("items", items);

        return "order-success";
    }

    /**
     * Xem lịch sử đơn hàng của tôi
     */
    @GetMapping("/orders")
    public String showMyOrders(Model model) {
        addCommonAttributes(model);
        User user = cartService.getDefaultUser();
        List<Order> orders = orderService.getOrdersByUser(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);

        return "my-orders";
    }
    @PostMapping("/orders/{orderCode}/cancel")
    public String cancelOrder(@PathVariable String orderCode,
                              @RequestParam(required = false, defaultValue = "") String reason,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = cartService.getDefaultUser();
            orderService.cancelOrder(user.getId(), orderCode, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn hàng " + orderCode + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hủy đơn hàng thất bại: " + e.getMessage());
        }
        return "redirect:/orders";
    }
}
