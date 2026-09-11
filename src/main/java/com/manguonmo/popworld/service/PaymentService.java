package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.SePayWebhookRequest;

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
}
