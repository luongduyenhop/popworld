package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.dto.request.RegisterRequest;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final UserService userService;

    /**
     * GET /register - Hiển thị form đăng ký
     */
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    /**
     * POST /register - Xử lý thông tin đăng ký
     */
    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        
        if (bindingResult.hasErrors()){
            return "register";
        }
        try {
            userService.register(request);
        }catch (Exception e){
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
        redirectAttributes.addFlashAttribute("successMessage","Đăng ký thành công vui lòng đăng nhập!");
        return "redirect:/login";
    }

    /**
     * GET /login - Hiển thị form đăng nhập
     */
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    /**
     * GET /403 - Trang thông báo từ chối truy cập (Access Denied)
     */
    @GetMapping("/403")
    public String showAccessDeniedPage() {
        return "403";
    }
}
