package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.response.InventorySummaryResponse;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminInventoryWebController {

    private final InventoryService inventoryService;

    @GetMapping
    public String viewInventory(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "10") int threshold,
            Model model) {

        int validThreshold = threshold > 0 ? threshold : 10;
        InventorySummaryResponse summary = inventoryService.getInventorySummary(validThreshold);
        List<Product> products = inventoryService.getInventoryProducts(status, keyword, validThreshold);

        model.addAttribute("summary", summary);
        model.addAttribute("products", products);
        model.addAttribute("currentStatus", status.toUpperCase());
        model.addAttribute("keyword", keyword);
        model.addAttribute("threshold", validThreshold);
        model.addAttribute("activeItem", "inventory");

        return "admin/inventory";
    }

    @PostMapping("/{id}/restock")
    public String restockProduct(
            @PathVariable Long id,
            @RequestParam int quantityToAdd,
            @RequestParam(required = false) String note,
            @RequestParam(required = false, defaultValue = "10") int threshold,
            RedirectAttributes redirectAttributes) {

        try {
            Product updated = inventoryService.quickRestock(id, quantityToAdd, note);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã nhập thêm +" + quantityToAdd + " sản phẩm vào kho cho [" + updated.getName() + "]. Tồn kho mới: " + updated.getStockQuantity() + " hộp.");
        } catch (Exception e) {
            log.error("Lỗi khi nhập thêm kho cho sản phẩm #{}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể nhập kho: " + e.getMessage());
        }

        return "redirect:/admin/inventory?threshold=" + threshold;
    }

    @PostMapping("/{id}/adjust")
    public String adjustProductStock(
            @PathVariable Long id,
            @RequestParam int newStockQuantity,
            @RequestParam(required = false, defaultValue = "Kiểm kê định kỳ") String reason,
            @RequestParam(required = false, defaultValue = "10") int threshold,
            RedirectAttributes redirectAttributes) {

        try {
            Product updated = inventoryService.adjustStock(id, newStockQuantity, reason);
            int validThreshold = threshold > 0 ? threshold : 10;

            String statusNotice;
            if (newStockQuantity <= 0) {
                statusNotice = " [HẾT HÀNG - Cạn kho]";
            } else if (newStockQuantity <= validThreshold) {
                statusNotice = " [SẮP HẾT HÀNG - Cảnh báo cần nhập thêm]";
            } else {
                statusNotice = " [AN TOÀN]";
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật tồn kho kiểm kê cho [" + updated.getName() + "] thành " + newStockQuantity + " hộp" + statusNotice + ". Lý do: " + reason);
        } catch (Exception e) {
            log.error("Lỗi khi điều chỉnh tồn kho sản phẩm #{}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể điều chỉnh tồn kho: " + e.getMessage());
        }

        return "redirect:/admin/inventory?threshold=" + threshold;
    }

    @GetMapping("/api/{id}/history")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<com.manguonmo.popworld.dto.response.InventoryLogResponse>> getStockHistory(
            @PathVariable Long id) {
        return org.springframework.http.ResponseEntity.ok(inventoryService.getProductStockHistory(id));
    }
}
