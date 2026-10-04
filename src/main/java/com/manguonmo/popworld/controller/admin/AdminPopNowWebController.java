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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/popnow")
@PreAuthorize("hasRole('ADMIN')")
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
        boolean isExistingSeries = popNowAdminService.isExistingSeriesWithStandardItems(productId);

        model.addAttribute("product", product);
        model.addAttribute("items", items);
        model.addAttribute("slots", slots);
        model.addAttribute("isExistingSeries", isExistingSeries);
        model.addAttribute("rarities", RarityType.values());
        model.addAttribute("activeItem", "popnow");
        model.addAttribute("pageTitle", "Cấu Hình Series: " + product.getName());
        return "admin/popnow-detail";
    }

    @PostMapping("/{productId}/sync-series-items")
    public String syncSeriesItems(@PathVariable Long productId,
                                  @RequestParam(defaultValue = "true") boolean overrideExisting,
                                  RedirectAttributes redirectAttributes) {
        try {
            int synced = popNowAdminService.syncSeriesStandardItems(productId, overrideExisting);
            if (synced > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã nạp và đồng bộ thành công " + synced + " mô hình chuẩn của Series!");
            } else {
                redirectAttributes.addFlashAttribute("infoMessage",
                        "Các mô hình chuẩn của Series này đã tồn tại đầy đủ trong danh sách.");
            }
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ mô hình chuẩn từ Series: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể nạp mô hình từ Series: " + e.getMessage());
        }
        return "redirect:/admin/popnow/" + productId;
    }

    @PostMapping("/{productId}/quick-generate-templates")
    public String quickGenerateTemplates(@PathVariable Long productId,
                                         @RequestParam(required = false) Integer count,
                                         RedirectAttributes redirectAttributes) {
        try {
            int created = popNowAdminService.quickGenerateTemplateItems(productId, count);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã tạo nhanh " + created + " mô hình mẫu cho bộ sưu tập mới! Bạn có thể chỉnh sửa tên và ảnh cho từng mô hình.");
        } catch (Exception e) {
            log.error("Lỗi khi tạo nhanh mô hình mẫu: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo nhanh mẫu: " + e.getMessage());
        }
        return "redirect:/admin/popnow/" + productId;
    }

    @PostMapping("/{productId}/init-slots")
    public String initSlots(@PathVariable Long productId,
                            @RequestParam(required = false) Integer slotCount,
                            RedirectAttributes redirectAttributes) {
        try {
            int created = popNowAdminService.initializeSlots(productId, slotCount);
            if (created > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã khởi tạo thành công " + created + " ô hộp mới cho Series này!");
            } else {
                redirectAttributes.addFlashAttribute("infoMessage",
                        "Các ô hộp của Series này đã tồn tại đầy đủ theo quy cách.");
            }
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo ô hộp POP NOW: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể khởi tạo ô hộp: " + e.getMessage());
        }
        return "redirect:/admin/popnow/" + productId;
    }

    @PostMapping("/{productId}/update-packaging")
    public String updatePackaging(@PathVariable Long productId,
                                  @RequestParam Integer boxesPerSet,
                                  RedirectAttributes redirectAttributes) {
        try {
            popNowAdminService.updateBoxesPerSet(productId, boxesPerSet);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật quy cách bộ hộp thành " + boxesPerSet + " hộp/set thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật quy cách: " + e.getMessage());
        }
        return "redirect:/admin/popnow/" + productId;
    }

    @PostMapping("/{productId}/cleanup-slots")
    public String cleanupSlots(@PathVariable Long productId,
                               @RequestParam Integer targetCount,
                               RedirectAttributes redirectAttributes) {
        try {
            int cleaned = popNowAdminService.cleanupExcessAvailableSlots(productId, targetCount);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã dọn dẹp " + cleaned + " ô trống vượt quá giới hạn " + targetCount + " ô!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể dọn dẹp ô hộp: " + e.getMessage());
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
