package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.SePayWebhookRequest;

/**
 * Interface Quản lý Xử lý Thanh toán
 * 
 * [GHI CHÚ QUAN TRỌNG]:
 * Đây là NGHIỆP VỤ CỐT LÕI (Core Business Logic) dành riêng cho BẠN tự tay hiện thực 
 * tại class PaymentServiceImpl.
 */
public interface PaymentService {

    /**
     * Xử lý webhook biến động số dư từ SePay
     * 
     * @param webhookData Dữ liệu giao dịch chuyển khoản do SePay gửi sang
     * @param authorizationHeader Header Authorization chứa API Key từ SePay để xác thực bảo mật
     * @return true nếu xử lý thành công và cập nhật đơn hàng; false nếu không hợp lệ
     */
    boolean processSePayWebhook(SePayWebhookRequest webhookData, String authorizationHeader);

    /**
     * Xử lý webhook biến động số dư từ SePay với xác thực chữ ký điện tử HMAC-SHA256
     *
     * @param webhookData Dữ liệu giao dịch đã parse
     * @param authorizationHeader Header Authorization fallback
     * @param rawPayload Chuỗi payload thô dùng để băm chữ ký
     * @param signatureHeader Chữ ký số từ header X-Signature hoặc X-SePay-Signature
     * @return true nếu chữ ký hợp lệ và xử lý thành công
     */
    default boolean processSePayWebhook(SePayWebhookRequest webhookData, String authorizationHeader, String rawPayload, String signatureHeader) {
        return processSePayWebhook(webhookData, authorizationHeader);
    }
}

