package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.OutOfStockException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.OrderService;
import com.manguonmo.popworld.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller xử lý toàn bộ luồng Đặt hàng (Checkout) & Thanh toán (Payment)
 */
@Slf4j
@Controller
public class CheckoutWebController {

    private final OrderService orderService;
    private final CartService cartService;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;
    private final UserService userService;

    @Value("${sepay.bank-code:MBBank}")
    private String sepayBankCode;

    @Value("${sepay.account-number:0355416208}")
    private String sepayAccountNumber;

    @Value("${sepay.account-name:POPWORLD OFFICIAL STORE}")
    private String sepayAccountName;

    public CheckoutWebController(OrderService orderService,
                                 CartService cartService,
                                 CategoryService categoryService,
                                 CharacterIpService characterIpService,
                                 UserService userService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.categoryService = categoryService;
        this.characterIpService = characterIpService;
        this.userService = userService;
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
    }

    /**
     * Hiển thị trang điền thông tin đặt hàng (Checkout)
     */
    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, RedirectAttributes redirectAttributes, Principal principal) {
        addCommonAttributes(model);
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn không hợp lệ hoặc đã bị vô hiệu hóa.");
            return "redirect:/login";
        }

        List<CartItem> cartItems = cartService.getCartItems(user.getId());

        if (cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi thanh toán!");
            return "redirect:/cart";
        }

        List<CartItem> selectedItems = cartItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsSelected()))
                .toList();

        if (selectedItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một sản phẩm trong giỏ hàng để tiến hành thanh toán!");
            return "redirect:/cart";
        }

        BigDecimal subtotal = cartService.calculateSelectedTotal(user.getId());
        BigDecimal shippingFee = subtotal.compareTo(BigDecimal.valueOf(500000)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(30000);
        BigDecimal totalAmount = subtotal.add(shippingFee);

        model.addAttribute("user", user);
        model.addAttribute("cartItems", selectedItems);
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
                             RedirectAttributes redirectAttributes,
                             Principal principal) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }
            User user = userService.getUserByEmail(principal.getName());
            if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn không hợp lệ hoặc đã bị vô hiệu hóa.");
                return "redirect:/login";
            }

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

        } catch (BadRequestException | OutOfStockException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/checkout";
        } catch (Exception e) {
            log.error("Lỗi không mong muốn trong quá trình xử lý đặt hàng: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hệ thống gặp sự cố trong quá trình xử lý đơn hàng. Vui lòng thử lại sau!");
            return "redirect:/checkout";
        }
    }

    /**
     * Màn hình thanh toán chuyển khoản qua VietQR SePay động
     */
    @GetMapping("/checkout/payment/{orderCode}")
    public String showPaymentQrPage(@PathVariable String orderCode, Model model, Principal principal) {
        addCommonAttributes(model);
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            return "redirect:/cart";
        }

        if (principal != null) {
            User user = userService.getUserByEmail(principal.getName());
            if (user != null && order.getUser() != null && !order.getUser().getId().equals(user.getId())) {
                return "redirect:/403";
            }
        }

        List<OrderItem> items = orderService.getOrderItems(order.getId());

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
    public String showOrderSuccessPage(@PathVariable String orderCode, Model model, Principal principal) {
        addCommonAttributes(model);
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            return "redirect:/";
        }

        if (principal != null) {
            User user = userService.getUserByEmail(principal.getName());
            if (user != null && order.getUser() != null && !order.getUser().getId().equals(user.getId())) {
                return "redirect:/403";
            }
        }

        List<OrderItem> items = orderService.getOrderItems(order.getId());
        model.addAttribute("order", order);
        model.addAttribute("items", items);

        return "order-success";
    }

    /**
     * Xem lịch sử đơn hàng của tôi
     */
    @GetMapping("/orders")
    public String showMyOrders(Model model, Principal principal) {
        addCommonAttributes(model);
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }
        List<Order> orders = orderService.getOrdersByUser(user.getId());

        // Lấy danh sách sản phẩm cho từng đơn hàng để hiển thị ảnh thumbnail và thông tin chi tiết
        Map<Long, List<OrderItem>> orderItemsMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, order -> orderService.getOrderItems(order.getId()), (a, b) -> a));

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("orderItemsMap", orderItemsMap);

        return "my-orders";
    }

    @PostMapping("/orders/{orderCode}/cancel")
    public String cancelOrder(@PathVariable String orderCode,
                              @RequestParam(required = false, defaultValue = "") String reason,
                              RedirectAttributes redirectAttributes,
                              Principal principal) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }
            User user = userService.getUserByEmail(principal.getName());
            if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản không hợp lệ hoặc đã bị vô hiệu hóa.");
                return "redirect:/login";
            }
            orderService.cancelOrder(user.getId(), orderCode, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn hàng " + orderCode + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hủy đơn hàng thất bại: " + e.getMessage());
        }
        return "redirect:/orders";
    }
}
