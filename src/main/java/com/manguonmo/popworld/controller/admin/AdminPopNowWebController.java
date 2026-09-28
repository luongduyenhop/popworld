package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.request.BlindBoxItemFormRequest;
import com.manguonmo.popworld.dto.response.PopNowAdminProductSummary;
import com.manguonmo.popworld.entity.BlindBoxItem;
import com.manguonmo.popworld.entity.BlindBoxSlot;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.RarityType;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.service.PopNowAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/popnow")
@RequiredArgsConstructor
public class AdminPopNowWebController {

    private final PopNowAdminService popNowAdminService;

    @GetMapping
    public String listPopNowProducts(Model model) {
        List<PopNowAdminProductSummary> summaries = popNowAdminService.getPopNowProductSummaries();
        model.addAttribute("summaries", summaries);
        model.addAttribute("activeItem", "popnow");
        model.addAttribute("pageTitle", "Cấu Hình POP NOW");
        return "admin/popnow-list";
    }

    @GetMapping("/{productId}")
    public String productConfigDetail(@PathVariable Long productId, Model model) {
        Product product = popNowAdminService.getProductForConfig(productId);
        List<BlindBoxItem> items = popNowAdminService.getItemsByProductId(productId);
        List<BlindBoxSlot> slots = popNowAdminService.getSlotsByProductId(productId);

        model.addAttribute("product", product);
        model.addAttribute("items", items);
        model.addAttribute("slots", slots);
        model.addAttribute("rarities", RarityType.values());
        model.addAttribute("activeItem", "popnow");
        model.addAttribute("pageTitle", "Cấu Hình Series: " + product.getName());
        return "admin/popnow-detail";
    }

    @PostMapping("/{productId}/init-slots")
    public String initSlots(@PathVariable Long productId, RedirectAttributes redirectAttributes) {
        try {
            int created = popNowAdminService.initializeStandardSlots(productId);
            if (created > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã khởi tạo thành công " + created + " ô hộp mới (vị trí 1-12)!");
            } else {
                redirectAttributes.addFlashAttribute("infoMessage",
                        "Tất cả 12 ô hộp tiêu chuẩn của Series này đã tồn tại đầy đủ.");
            }
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo ô hộp POP NOW: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể khởi tạo ô hộp: " + e.getMessage());
        }
        return "redirect:/admin/popnow/" + productId;
    }

    @PostMapping("/{productId}/items")
    public String createItem(@PathVariable Long productId,
                             @Valid @ModelAttribute("itemForm") BlindBoxItemFormRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ: " + firstError);
            return "redirect:/admin/popnow/" + productId;
        }

        try {
            BlindBoxItem saved = popNowAdminService.saveItem(productId, null, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thêm mô hình \"" + saved.getName() + "\" vào Series thành công!");
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi thêm BlindBoxItem: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/admin/popnow/" + productId;
    }

    @PostMapping("/{productId}/items/{itemId}")
    public String updateItem(@PathVariable Long productId,
                             @PathVariable Long itemId,
                             @Valid @ModelAttribute("itemForm") BlindBoxItemFormRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ: " + firstError);
            return "redirect:/admin/popnow/" + productId;
        }

        try {
            BlindBoxItem saved = popNowAdminService.saveItem(productId, itemId, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật mô hình \"" + saved.getName() + "\" thành công!");
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật BlindBoxItem: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/admin/popnow/" + productId;
    }

    @PostMapping("/{productId}/items/{itemId}/toggle")
    public String toggleItemActive(@PathVariable Long productId,
                                   @PathVariable Long itemId,
                                   RedirectAttributes redirectAttributes) {
        try {
            popNowAdminService.toggleItemActive(productId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái kích hoạt thành công!");
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi chuyển trạng thái BlindBoxItem: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/admin/popnow/" + productId;
    }

    @PostMapping("/{productId}/items/{itemId}/delete")
    public String deleteItem(@PathVariable Long productId,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            popNowAdminService.deleteItem(productId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa mô hình thành công!");
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi xóa BlindBoxItem: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa mô hình: " + e.getMessage());
        }

        return "redirect:/admin/popnow/" + productId;
    }
}
