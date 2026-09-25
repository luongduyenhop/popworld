package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.CouponValidateRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.CouponDiscountResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.CouponService;
import com.manguonmo.popworld.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponApiControllerTest {

    @Mock
    private CouponService couponService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CouponApiController couponApiController;

    @Test
    @DisplayName("validateCoupon thành công: Trả về 200 OK và kết quả tính tiền giảm giá khi có user đăng nhập")
    void validateCoupon_Success_ReturnsDiscountResponse() {
        User user = User.builder().id(1L).fullName("Nguyen Van A").build();
        Principal principal = () -> "user@popworld.com";
        when(userService.getUserByEmail("user@popworld.com")).thenReturn(user);

        CouponDiscountResponse mockResponse = CouponDiscountResponse.builder()
                .couponCode("POP10")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("10"))
                .discountAmount(new BigDecimal("50000"))
                .subtotal(new BigDecimal("500000"))
                .newTotal(new BigDecimal("450000"))
                .message("Áp dụng mã thành công")
                .build();

        when(couponService.calculateDiscount("POP10", 1L, new BigDecimal("500000")))
                .thenReturn(mockResponse);

        CouponValidateRequest request = CouponValidateRequest.builder()
                .couponCode("POP10")
                .subtotal(new BigDecimal("500000"))
                .build();

        ResponseEntity<ApiResponse<CouponDiscountResponse>> response = couponApiController.validateCoupon(request, principal);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Áp dụng mã giảm giá thành công", response.getBody().getMessage());
        assertEquals("POP10", response.getBody().getData().getCouponCode());
        assertEquals(new BigDecimal("50000"), response.getBody().getData().getDiscountAmount());
    }

    @Test
    @DisplayName("validateCoupon thành công khi principal null (khách vãng lai)")
    void validateCoupon_Success_WhenPrincipalNull() {
        CouponDiscountResponse mockResponse = CouponDiscountResponse.builder()
                .couponCode("FREESHIP")
                .discountAmount(new BigDecimal("30000"))
                .build();

        when(couponService.calculateDiscount("FREESHIP", null, new BigDecimal("600000")))
                .thenReturn(mockResponse);

        CouponValidateRequest request = CouponValidateRequest.builder()
                .couponCode("FREESHIP")
                .subtotal(new BigDecimal("600000"))
                .build();

        ResponseEntity<ApiResponse<CouponDiscountResponse>> response = couponApiController.validateCoupon(request, null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("FREESHIP", response.getBody().getData().getCouponCode());
        verify(couponService, times(1)).calculateDiscount("FREESHIP", null, new BigDecimal("600000"));
        verifyNoInteractions(userService);
    }
}
