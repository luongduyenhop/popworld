package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.dto.response.BlindBoxItemResponse;
import com.manguonmo.popworld.dto.response.BlindBoxSlotResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;
import com.manguonmo.popworld.entity.BoxReservation;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.ReservationStatus;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.BoxReservationRepository;
import com.manguonmo.popworld.repository.OwnedItemRepository;
import com.manguonmo.popworld.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopNowWebControllerTest {

    @Mock
    private PopNowService popNowService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CharacterIpService characterIpService;

    @Mock
    private CartService cartService;

    @Mock
    private BoxReservationRepository boxReservationRepository;

    @Mock
    private OwnedItemRepository ownedItemRepository;

    @Mock
    private UserAddressService userAddressService;

    @Mock
    private PopNowThemeService popNowThemeService;

    @Mock
    private Model model;

    @Mock
    private Principal principal;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private PopNowWebController controller;

    private User sampleUser;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user@test.com").fullName("Test User").enabled(true).build();
        sampleProduct = Product.builder().id(10L).name("Hirono Little Mischief").slug("hirono-little-mischief")
                .active(true).singlePrice(BigDecimal.valueOf(350000)).build();
        lenient().when(popNowThemeService.getThemeForProduct(any())).thenReturn(null);
    }

    @Test
    @DisplayName("popNowCatalog: Hiển thị danh mục POP NOW")
    void popNowCatalog_ReturnsCatalogView() {
        when(productService.getProductsByCategorySlug("blind-box")).thenReturn(List.of(sampleProduct));

        String view = controller.popNowCatalog(model, null);

        assertEquals("popnow-catalog", view);
        verify(model).addAttribute(eq("products"), any());
        verify(model).addAttribute(eq("pageTitle"), any());
    }

    @Test
    @DisplayName("pickBoxPage: Tải thông tin sản phẩm và danh sách 12 ô hộp thành công")
    void pickBoxPage_Success() {
        when(productService.getProductBySlug("hirono-little-mischief")).thenReturn(Optional.of(sampleProduct));
        when(popNowService.getProductSlots(10L)).thenReturn(List.of(
                BlindBoxSlotResponse.builder().slotIndex(1).status("AVAILABLE").build()
        ));
        when(popNowService.getSeriesItems(10L)).thenReturn(Collections.emptyList());

        String view = controller.pickBoxPage("hirono-little-mischief", model, null);

        assertEquals("popnow-pick", view);
        verify(model).addAttribute("product", sampleProduct);
        verify(model).addAttribute(eq("slots"), any());
        verify(model).addAttribute(eq("seriesItems"), any());
    }

    @Test
    @DisplayName("pickBoxPage: Sản phẩm không tồn tại -> Chuyển hướng về /popnow")
    void pickBoxPage_NotFound_Redirects() {
        when(productService.getProductBySlug("non-existent")).thenReturn(Optional.empty());

        String view = controller.pickBoxPage("non-existent", model, null);

        assertEquals("redirect:/popnow", view);
    }

    @Test
    @DisplayName("checkoutReservation: Chưa đăng nhập -> Chuyển hướng sang /login")
    void checkoutReservation_Unauthenticated_RedirectsLogin() {
        String view = controller.checkoutReservation("PN-123", "SEPAY", redirectAttributes, null);
        assertEquals("redirect:/login", view);
    }

    @Test
    @DisplayName("checkoutReservation: Tạo đơn thành công với SEPAY -> Chuyển hướng tới trang thanh toán VietQR")
    void checkoutReservation_Success_RedirectsToPayment() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        Order mockOrder = Order.builder().orderCode("PW-123456").paymentMethod("SEPAY").build();
        when(orderService.createOrderForReservation(1L, "PN-RES-01", "SEPAY")).thenReturn(mockOrder);

        String view = controller.checkoutReservation("PN-RES-01", "SEPAY", redirectAttributes, principal);

        assertEquals("redirect:/checkout/payment/PW-123456", view);
    }

    @Test
    @DisplayName("revealPage: Người dùng chưa đăng nhập -> Chuyển hướng sang /login")
    void revealPage_Unauthenticated_RedirectsLogin() {
        String view = controller.revealPage("PN-RES-01", model, null);
        assertEquals("redirect:/login", view);
    }

    @Test
    @DisplayName("revealPage: IDOR Protection - Truy cập phiếu của người khác -> Chuyển hướng 403")
    void revealPage_IDOR_Redirects403() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        User otherUser = User.builder().id(999L).build();
        BoxReservation otherReservation = BoxReservation.builder()
                .reservationCode("PN-OTHER")
                .user(otherUser)
                .build();
        when(boxReservationRepository.findByReservationCode("PN-OTHER")).thenReturn(Optional.of(otherReservation));

        String view = controller.revealPage("PN-OTHER", model, principal);

        assertEquals("redirect:/403", view);
    }

    @Test
    @DisplayName("revealPage: Chủ sở hữu hợp lệ -> Hiển thị trang popnow-reveal")
    void revealPage_Owner_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        BoxReservation reservation = BoxReservation.builder()
                .reservationCode("PN-MINE")
                .user(sampleUser)
                .product(sampleProduct)
                .status(ReservationStatus.PURCHASED)
                .build();
        when(boxReservationRepository.findByReservationCode("PN-MINE")).thenReturn(Optional.of(reservation));

        String view = controller.revealPage("PN-MINE", model, principal);

        assertEquals("popnow-reveal", view);
        verify(model).addAttribute("reservation", reservation);
    }

    @Test
    @DisplayName("myCabinet: Hiển thị tủ đồ ảo của người dùng đăng nhập")
    void myCabinet_Success() {
        when(principal.getName()).thenReturn("user@test.com");
        when(userService.getUserByEmail("user@test.com")).thenReturn(sampleUser);

        when(popNowService.getUserCabinet(1L)).thenReturn(List.of(
                OwnedItemResponse.builder().id(1L).itemName("Molly Space").rarity("SECRET").build(),
                OwnedItemResponse.builder().id(2L).itemName("Labubu").rarity("REGULAR").build()
        ));
        when(userAddressService.getAddressesByUserId(1L)).thenReturn(Collections.emptyList());

        String view = controller.myCabinet(model, principal);

        assertEquals("popnow-cabinet", view);
        verify(model).addAttribute("totalCount", 2);
        verify(model).addAttribute("secretCount", 1L);
        verify(model).addAttribute("regularCount", 1L);
        verify(model).addAttribute("userAddresses", Collections.emptyList());
    }
}
