package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.request.PointSettingUpdateRequest;
import com.manguonmo.popworld.dto.response.CustomerStatsResponse;
import com.manguonmo.popworld.dto.response.PointTransactionResponse;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.entity.UserAddress;
import com.manguonmo.popworld.service.OrderService;
import com.manguonmo.popworld.service.PointSettingService;
import com.manguonmo.popworld.service.UserAddressService;
import com.manguonmo.popworld.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCustomerWebController {

    private final UserService userService;
    private final UserAddressService userAddressService;
    private final OrderService orderService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PointSettingService pointSettingService;

    @GetMapping
    public String listCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String tier,
            @RequestParam(required = false) Boolean status,
            Model model) {
        boolean hasFilter = (keyword != null && !keyword.isBlank()) ||
                            (tier != null && !"ALL".equalsIgnoreCase(tier)) ||
                            status != null;
        List<User> customers = hasFilter 
                ? userService.searchCustomers(keyword, tier, status)
                : userService.getAllCustomers();
        CustomerStatsResponse stats = userService.getCustomerStats();

        model.addAttribute("customers", customers);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentTier", tier != null ? tier.toUpperCase() : "ALL");
        model.addAttribute("currentStatus", status);
        model.addAttribute("totalCustomers", stats.getTotalCustomers());
        model.addAttribute("vipCount", stats.getVipCount());
        model.addAttribute("memberCount", stats.getMemberCount());
        model.addAttribute("pointSetting", pointSettingService != null ? pointSettingService.getSetting() : null);
        model.addAttribute("activeNav", "customers");

        return "admin/customers";
    }

    @PostMapping("/point-settings")
    public String updatePointSettings(
            @Valid @ModelAttribute PointSettingUpdateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ: " + errorMsg);
            return "redirect:/admin/customers";
        }
        try {
            if (pointSettingService != null) {
                pointSettingService.updateSetting(request);
                redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật cấu hình điểm thưởng thành công!");
            }
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật cấu hình điểm thưởng: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật cấu hình: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    public String listCustomers(Model model) {
        return listCustomers(null, "ALL", null, model);
    }

    @GetMapping("/{id}")
    public String viewCustomerDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User customer = userService.getUserById(id);
            List<UserAddress> addresses = userAddressService.getAddressesByUserId(id);
            List<Order> orders = orderService.getOrdersByUser(id);
            List<PointTransactionResponse> pointHistory = userService.getPointHistory(id);

            model.addAttribute("customer", customer);
            model.addAttribute("addresses", addresses);
            model.addAttribute("orders", orders);
            model.addAttribute("pointHistory", pointHistory);
            model.addAttribute("activeNav", "customers");

            return "admin/customer-detail";
        } catch (Exception e) {
            log.error("Lỗi khi tải chi tiết khách hàng #{}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tìm thấy khách hàng #" + id);
            return "redirect:/admin/customers";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleCustomerStatus(@PathVariable Long id,
                                       @RequestParam(value = "redirect", required = false, defaultValue = "detail") String redirect,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        try {
            User target = userService.getUserById(id);
            if (principal != null && principal.getName().equalsIgnoreCase(target.getEmail())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không thể tự khóa tài khoản Quản trị viên đang đăng nhập!");
                return "redirect:/admin/customers/" + id;
            }

            User updated = userService.toggleUserStatus(id);
            String statusText = Boolean.TRUE.equals(updated.getEnabled()) ? "kích hoạt mở khóa" : "tạm khóa";
            redirectAttributes.addFlashAttribute("successMessage", "Đã " + statusText + " tài khoản thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi đổi trạng thái khách hàng #{}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        if ("list".equalsIgnoreCase(redirect)) {
            return "redirect:/admin/customers";
        }
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/{id}/adjust-points")
    public String adjustPoints(@PathVariable Long id,
                               @RequestParam(value = "rewardPointsDelta", required = false, defaultValue = "0") Integer rewardPointsDelta,
                               @RequestParam(value = "luckyPointsDelta", required = false, defaultValue = "0") Integer luckyPointsDelta,
                               @RequestParam(value = "hintCardsDelta", required = false, defaultValue = "0") Integer hintCardsDelta,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.adjustUserPoints(id, rewardPointsDelta, luckyPointsDelta, hintCardsDelta);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật điểm và thẻ bài may mắn cho khách hàng #" + id + " thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi điều chỉnh điểm khách hàng #{}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể điều chỉnh điểm: " + e.getMessage());
        }
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/{id}/reset-password")
    public String resetCustomerPassword(@PathVariable Long id,
                                        @RequestParam("newPassword") String newPassword,
                                        @RequestParam("confirmPassword") String confirmPassword,
                                        @RequestParam(value = "redirect", required = false, defaultValue = "detail") String redirect,
                                        Principal principal,
                                        RedirectAttributes redirectAttributes) {
        String adminEmail = principal != null ? principal.getName() : "Admin";
        try {
            userService.adminResetPassword(id, newPassword, confirmPassword, adminEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đặt lại mật khẩu cho khách hàng #" + id + " thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi admin đặt lại mật khẩu cho khách hàng #{}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể đặt lại mật khẩu: " + e.getMessage());
        }

        if ("list".equalsIgnoreCase(redirect)) {
            return "redirect:/admin/customers";
        }
        return "redirect:/admin/customers/" + id;
    }
}
