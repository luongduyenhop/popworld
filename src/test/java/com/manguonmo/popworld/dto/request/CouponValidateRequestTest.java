package com.manguonmo.popworld.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CouponValidateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid CouponValidateRequest: Không có vi phạm ràng buộc")
    void validRequest_NoViolations() {
        CouponValidateRequest request = CouponValidateRequest.builder()
                .couponCode("POP10")
                .subtotal(new BigDecimal("500000"))
                .build();

        Set<ConstraintViolation<CouponValidateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Invalid subtotal âm: Vi phạm @PositiveOrZero")
    void negativeSubtotal_HasViolation() {
        CouponValidateRequest request = CouponValidateRequest.builder()
                .couponCode("POP10")
                .subtotal(new BigDecimal("-10000"))
                .build();

        Set<ConstraintViolation<CouponValidateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("không được âm")));
    }

    @Test
    @DisplayName("Invalid subtotal null: Vi phạm @NotNull")
    void nullSubtotal_HasViolation() {
        CouponValidateRequest request = CouponValidateRequest.builder()
                .couponCode("POP10")
                .subtotal(null)
                .build();

        Set<ConstraintViolation<CouponValidateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("không được để trống")));
    }

    @Test
    @DisplayName("Invalid couponCode rỗng: Vi phạm @NotBlank")
    void blankCouponCode_HasViolation() {
        CouponValidateRequest request = CouponValidateRequest.builder()
                .couponCode("   ")
                .subtotal(new BigDecimal("100000"))
                .build();

        Set<ConstraintViolation<CouponValidateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("nhập mã giảm giá")));
    }
}
