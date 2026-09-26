package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.OrderItem;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.entity.UserAddress;
import com.manguonmo.popworld.repository.CouponRepository;
import com.manguonmo.popworld.repository.UserAddressRepository;
import com.manguonmo.popworld.dto.request.AddressRequest;
import com.manguonmo.popworld.dto.request.ChangePasswordRequest;
import com.manguonmo.popworld.dto.request.ProfileUpdateRequest;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller quản lý Trung tâm Tài khoản Khách hàng (Account Hub)
 * Canonical URL: /account
 * Alias redirect: /profile -> /account
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AccountWebController {

    private final UserService userService;
    private final OrderService orderService;
    private final CouponRepository couponRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserAddressService userAddressService;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;

    private void addCommonAttributes(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
    }


    /**
     * Canonical Account Hub
     * Hỗ trợ alias /orders tạm thời để duy trì tương thích cho đến khi tách riêng trang đơn hàng
     */
    @GetMapping({"/account", "/orders"})
    public String showAccountHub(Model model, Principal principal) {
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

        List<UserAddress> addresses = userAddressRepository.findByUserId(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("orderItemsMap", orderItemsMap);
        model.addAttribute("addresses", addresses);
        model.addAttribute("couponsCount", couponRepository.countByActiveTrue());

        return "my-orders";
    }

    /**
     * Alias redirect từ /profile về /account canonical
     */
    @GetMapping("/profile")
    public String profileRedirect() {
        return "redirect:/account";
    }

    /**
     * Chi tiết đơn hàng cho khách hàng
     * Hỗ trợ cả /orders/{orderCode} và /account/orders/{orderCode}
     */
    @GetMapping({"/orders/{orderCode}", "/account/orders/{orderCode}"})
    public String showOrderDetailPage(@PathVariable String orderCode, Model model, Principal principal) {
        addCommonAttributes(model);
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            return "redirect:/account";
        }

        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByEmail(principal.getName());
        boolean isAdmin = user != null && ("ROLE_ADMIN".equals(user.getRole()) || "ADMIN".equals(user.getRole()));
        boolean isOwner = user != null && order.getUser() != null && order.getUser().getId().equals(user.getId());
        if (!isAdmin && !isOwner) {
            return "redirect:/403";
        }

        List<OrderItem> items = orderService.getOrderItems(order.getId());
        model.addAttribute("order", order);
        model.addAttribute("items", items);
        return "order-success";
    }

    /**
     * Cập nhật thông tin tài khoản (Họ tên, SĐT)
     */
    @PostMapping("/account/profile")
    public String updateProfile(@Valid @ModelAttribute("profileRequest") ProfileUpdateRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("Dữ liệu cập nhật không hợp lệ");
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/account";
        }
        try {
            userService.updateProfile(user.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin tài khoản thành công!");
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật thông tin cá nhân: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật thông tin. Vui lòng thử lại!");
        }
        return "redirect:/account";
    }

    /**
     * Đổi mật khẩu tài khoản
     */
    @PostMapping("/account/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("Dữ liệu đổi mật khẩu không hợp lệ");
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/account";
        }
        try {
            userService.changePassword(user.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi đổi mật khẩu: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể đổi mật khẩu. Vui lòng thử lại!");
        }
        return "redirect:/account";
    }

    /**
     * Thêm địa chỉ mới vào sổ địa chỉ
     */
    @PostMapping("/account/addresses")
    public String addAddress(@Valid @ModelAttribute("addressRequest") AddressRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("Thông tin địa chỉ không hợp lệ");
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/account";
        }
        try {
            userAddressService.createAddress(user.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm địa chỉ mới thành công!");
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi thêm địa chỉ: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể lưu địa chỉ. Vui lòng thử lại!");
        }
        return "redirect:/account";
    }

    /**
     * Cập nhật địa chỉ trong sổ địa chỉ
     */
    @PostMapping("/account/addresses/{id}/edit")
    public String editAddress(@PathVariable Long id,
                              @Valid @ModelAttribute("addressRequest") AddressRequest request,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("Thông tin cập nhật địa chỉ không hợp lệ");
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/account";
        }
        try {
            userAddressService.updateAddress(user.getId(), id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật địa chỉ thành công!");
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi sửa địa chỉ: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật địa chỉ. Vui lòng thử lại!");
        }
        return "redirect:/account";
    }

    /**
     * Xóa địa chỉ khỏi sổ địa chỉ
     */
    @PostMapping("/account/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id,
                                RedirectAttributes redirectAttributes,
                                Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }
        try {
            userAddressService.deleteAddress(user.getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa địa chỉ thành công!");
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi xóa địa chỉ: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa địa chỉ. Vui lòng thử lại!");
        }
        return "redirect:/account";
    }

    /**
     * Đặt địa chỉ làm mặc định
     */
    @PostMapping("/account/addresses/{id}/default")
    public String setDefaultAddress(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes,
                                    Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return "redirect:/login";
        }
        try {
            userAddressService.setDefaultAddress(user.getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đặt địa chỉ làm mặc định!");
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi đặt địa chỉ mặc định: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể đặt địa chỉ mặc định. Vui lòng thử lại!");
        }
        return "redirect:/account";
    }
}

