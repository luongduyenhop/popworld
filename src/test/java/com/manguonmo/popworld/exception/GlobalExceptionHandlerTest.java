package com.manguonmo.popworld.exception;

import com.manguonmo.popworld.dto.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleValidationException: Trả về HTTP 400 và thông báo lỗi validation")
    void handleValidationException_ReturnsBadRequestWithValidationMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "subtotal", "Giá trị đơn hàng không được âm"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<?>> response = handler.handleValidationException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Giá trị đơn hàng không được âm", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleValidationException: Dùng default message nếu FieldError rỗng")
    void handleValidationException_EmptyMessage_ReturnsDefault() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "field", ""));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<?>> response = handler.handleValidationException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Dữ liệu đầu vào không hợp lệ", response.getBody().getMessage());
    }

    @Test
    @DisplayName("resourceNotFoundException: Trả về HTTP 404")
    void resourceNotFoundException_ReturnsNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Không tìm thấy sản phẩm");
        ResponseEntity<ApiResponse<?>> response = handler.resourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Không tìm thấy sản phẩm", response.getBody().getMessage());
    }

    @Test
    @DisplayName("outOfStockException: Trả về HTTP 400")
    void outOfStockException_ReturnsBadRequest() {
        OutOfStockException ex = new OutOfStockException("Hết hàng trong kho");
        ResponseEntity<ApiResponse<?>> response = handler.outOfStockException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Hết hàng trong kho", response.getBody().getMessage());
    }

    @Test
    @DisplayName("badRequestException: Trả về HTTP 400")
    void badRequestException_ReturnsBadRequest() {
        BadRequestException ex = new BadRequestException("Tham số không hợp lệ");
        ResponseEntity<ApiResponse<?>> response = handler.badRequestException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Tham số không hợp lệ", response.getBody().getMessage());
    }

    @Test
    @DisplayName("exception: Trả về HTTP 500 và ẩn chi tiết nội bộ")
    void exception_ReturnsInternalServerErrorAndMasksMessage() {
        Exception ex = new NullPointerException("Null reference inside internal logic");
        ResponseEntity<ApiResponse<?>> response = handler.exception(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Đã xảy ra lỗi nội bộ từ hệ thống. Vui lòng thử lại sau!", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("Null reference"));
    }
}
