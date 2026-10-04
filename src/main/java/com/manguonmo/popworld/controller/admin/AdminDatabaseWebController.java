package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.service.DatabaseSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/database")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDatabaseWebController {

    private final DatabaseSeedService databaseSeedService;

    @PostMapping("/reseed")
    public String reseedOfficialData(RedirectAttributes redirectAttributes) {
        log.info("Admin kích hoạt đồng bộ / tái thiết lập dữ liệu chuẩn POP MART");
        try {
            databaseSeedService.reseedOfficialPopMartData();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã đồng bộ và làm mới toàn bộ danh mục, nhân vật, khay hộp (6/9/12) và đánh giá chuẩn POP MART thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ dữ liệu POP MART: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Đồng bộ dữ liệu thất bại: " + e.getMessage());
        }
        return "redirect:/admin/popnow";
    }
}
