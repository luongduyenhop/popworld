package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.dto.response.BlindBoxItemResponse;
import com.manguonmo.popworld.dto.response.BlindBoxSlotResponse;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.repository.BoxReservationRepository;
import com.manguonmo.popworld.repository.OwnedItemRepository;
import com.manguonmo.popworld.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/popnow")
public class PopNowWebController {

    private final PopNowService popNowService;
    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;
    private final CartService cartService;
    private final BoxReservationRepository boxReservationRepository;
    private final OwnedItemRepository ownedItemRepository;
    private final UserAddressService userAddressService;

    public PopNowWebController(PopNowService popNowService,
                               ProductService productService,
                               OrderService orderService,
                               UserService userService,
                               CategoryService categoryService,
                               CharacterIpService characterIpService,
                               CartService cartService,
                               BoxReservationRepository boxReservationRepository,
                               OwnedItemRepository ownedItemRepository,
                               UserAddressService userAddressService) {
        this.popNowService = popNowService;
        this.productService = productService;
        this.orderService = orderService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.characterIpService = characterIpService;
        this.cartService = cartService;
        this.boxReservationRepository = boxReservationRepository;
        this.ownedItemRepository = ownedItemRepository;
        this.userAddressService = userAddressService;
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
        int cartCount = 0;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                User user = userService.getUserByEmail(auth.getName());
                if (user != null) {
                    cartCount = cartService.getCartCount(user.getId());
                }
            }
        } catch (Exception ignored) {
        }
        model.addAttribute("cartCount", cartCount);
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.getUserByEmail(principal.getName());
    }

    /**
     * Danh mục sản phẩm hỗ trợ bóc trực tuyến POP NOW
     */
    @GetMapping
    public String popNowCatalog(Model model) {
        addCommonAttributes(model);
        List<Product> products = productService.getProductsByCategorySlug("blind-box");
        if (products.isEmpty()) {
            products = productService.getAllActiveProducts();
        }
        model.addAttribute("products", products);
        model.addAttribute("pageTitle", "POP NOW - Bóc Hộp Online");
        return "popnow-catalog";
    }

    /**
     * Màn hình chọn ô hộp Pick-a-Box tương tác thực tế
     */
    @GetMapping("/pick/{slug}")
    public String pickBoxPage(@PathVariable String slug, Model model, Principal principal) {
        addCommonAttributes(model);
        Optional<Product> productOpt = productService.getProductBySlug(slug);
        if (productOpt.isEmpty()) {
            return "redirect:/popnow";
        }
        Product product = productOpt.get();
        if (!Boolean.TRUE.equals(product.getActive())) {
            return "redirect:/popnow";
        }

        List<BlindBoxSlotResponse> slots = popNowService.getProductSlots(product.getId());
        List<BlindBoxItemResponse> seriesItems = popNowService.getSeriesItems(product.getId());

        User currentUser = getAuthenticatedUser(principal);
        BoxReservationResponse activeReservation = null;

        if (currentUser != null) {
            List<BoxReservation> userReservations = boxReservationRepository.findByUserIdAndStatus(
                    currentUser.getId(), ReservationStatus.RESERVED);
            for (BoxReservation res : userReservations) {
                if (res.getProduct().getId().equals(product.getId()) && !res.isExpired()) {
                    activeReservation = popNowService.getReservationByCode(currentUser.getId(), res.getReservationCode());
                    break;
                }
            }
        }

        String hashPart = Integer.toHexString(Math.abs((product.getId().toString() + product.getSlug()).hashCode())).toUpperCase();
        String setCode = "No." + (hashPart.length() >= 6 ? hashPart.substring(0, 6) : String.format("%-6s", hashPart).replace(' ', 'X'));

        model.addAttribute("product", product);
        model.addAttribute("slots", slots);
        model.addAttribute("seriesItems", seriesItems);
        model.addAttribute("activeReservation", activeReservation);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("setCode", setCode);
        return "popnow-pick";
    }

    /**
     * Handoff thanh toán: Tạo Order liên kết với phiếu giữ hộp và chuyển đến trang thanh toán VietQR SePay
     */
    @PostMapping("/checkout/{reservationCode}")
    public String checkoutReservation(@PathVariable String reservationCode,
                                      @RequestParam(required = false, defaultValue = "SEPAY") String paymentMethod,
                                      RedirectAttributes redirectAttributes,
                                      Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Order order = orderService.createOrderForReservation(user.getId(), reservationCode, paymentMethod);
            if ("SEPAY".equalsIgnoreCase(order.getPaymentMethod())) {
                return "redirect:/checkout/payment/" + order.getOrderCode();
            } else {
                return "redirect:/checkout/success/" + order.getOrderCode();
            }
        } catch (Exception e) {
            log.error("Lỗi khi tạo đơn hàng POP NOW: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/popnow";
        }
    }

    /**
     * Màn hình khui seal và lật mở kết quả (Unbox Reveal)
     */
    @GetMapping("/reveal/{reservationCode}")
    public String revealPage(@PathVariable String reservationCode, Model model, Principal principal) {
        addCommonAttributes(model);
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        BoxReservation reservation = boxReservationRepository.findByReservationCode(reservationCode)
                .orElse(null);

        if (reservation == null) {
            return "redirect:/popnow";
        }

        // Kiểm tra quyền sở hữu IDOR
        if (!reservation.getUser().getId().equals(user.getId())) {
            return "redirect:/403";
        }

        OwnedItem ownedItem = null;
        if (reservation.getStatus() == ReservationStatus.UNBOXED) {
            ownedItem = ownedItemRepository.findByReservationId(reservation.getId()).orElse(null);
        }

        model.addAttribute("reservation", reservation);
        model.addAttribute("product", reservation.getProduct());
        model.addAttribute("ownedItem", ownedItem);
        return "popnow-reveal";
    }

    /**
     * Tủ trưng bày mô hình cá nhân (Virtual Cabinet)
     */
    @GetMapping("/cabinet")
    public String myCabinet(Model model, Principal principal) {
        addCommonAttributes(model);
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return "redirect:/login";
        }

        List<OwnedItemResponse> cabinetItems = popNowService.getUserCabinet(user.getId());
        long secretCount = cabinetItems.stream().filter(item -> "SECRET".equalsIgnoreCase(item.getRarity())).count();
        long regularCount = cabinetItems.size() - secretCount;

        List<UserAddress> userAddresses = userAddressService != null ? userAddressService.getAddressesByUserId(user.getId()) : java.util.Collections.emptyList();

        model.addAttribute("cabinetItems", cabinetItems);
        model.addAttribute("userAddresses", userAddresses);
        model.addAttribute("totalCount", cabinetItems.size());
        model.addAttribute("secretCount", secretCount);
        model.addAttribute("regularCount", regularCount);
        model.addAttribute("pageTitle", "Tủ Đồ Ảo - Virtual Cabinet");
        return "popnow-cabinet";
    }
}
