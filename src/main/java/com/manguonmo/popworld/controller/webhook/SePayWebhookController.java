package com.manguonmo.popworld.controller.webhook;

import com.manguonmo.popworld.dto.request.SePayWebhookRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller tiếp nhận tín hiệu Webhook từ cổng thanh toán SePay (sepay.vn).
 * 
 * Khi người dùng chuyển khoản quét mã VietQR thành công, SePay sẽ gửi một HTTP POST
 * kèm thông tin giao dịch đến endpoint này.
 * 
 * Hỗ trợ xác thực kép (Dual Verification):
 * 1. Chữ ký điện tử HMAC-SHA256 (qua header X-Signature hoặc X-SePay-Signature)
 * 2. API Key (qua header Authorization)
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/sepay")
public class SePayWebhookController {

    private final PaymentService paymentService;

    @Autowired
    public SePayWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handleSePayWebhook(
            @RequestBody(required = false) SePayWebhookRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Signature", required = false) String signatureHeader,
            @RequestHeader(value = "X-SePay-Signature", required = false) String sepaySigHeader) {

        if (paymentService == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(ApiResponse.error("PaymentServiceImpl chưa được cấu hình!"));
        }

        String effectiveSig = (signatureHeader != null && !signatureHeader.isBlank())
                ? signatureHeader
                : sepaySigHeader;

        boolean success;
        if (effectiveSig != null && !effectiveSig.isBlank()) {
            success = paymentService.processSePayWebhook(request, authHeader, null, effectiveSig);
        } else {
            success = paymentService.processSePayWebhook(request, authHeader);
        }

        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Xử lý webhook SePay thành công.", null));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu webhook không hợp lệ hoặc xử lý thất bại."));
        }
    }
}
