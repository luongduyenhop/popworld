package com.manguonmo.popworld.controller;

import com.manguonmo.popworld.dto.SePayWebhookRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller tiếp nhận tín hiệu Webhook từ cổng thanh toán SePay (sepay.vn).
 * 
 * Khi người dùng chuyển khoản quét mã VietQR thành công, SePay sẽ gửi một HTTP POST
 * kèm thông tin giao dịch đến endpoint này.
 */
@RestController
@RequestMapping("/api/payment/sepay")
public class SePayWebhookController {

    private final PaymentService paymentService;

    // Sử dụng required = false để ứng dụng vẫn khởi động mượt mà khi bạn đang chuẩn bị viết PaymentServiceImpl
    public SePayWebhookController(@Autowired(required = false) PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handleSePayWebhook(
            @RequestBody SePayWebhookRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (paymentService == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ApiResponse.error("PaymentServiceImpl chưa được cấu hình!"));
        }

        boolean success = paymentService.processSePayWebhook(request, authHeader);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Xử lý webhook SePay thành công.",null));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu webhook không hợp lệ hoặc xử lý thất bại."));
        }
    }
}
