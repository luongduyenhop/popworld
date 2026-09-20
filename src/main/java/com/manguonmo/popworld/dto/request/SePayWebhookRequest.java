package com.manguonmo.popworld.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) đại diện cho dữ liệu Webhook gửi từ cổng SePay (sepay.vn)
 * khi có biến động số dư chuyển khoản ngân hàng qua VietQR.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SePayWebhookRequest {

    // ID giao dịch trên hệ thống SePay
    private Long id;

    // Ngân hàng nhận tiền (MBBank, Vietcombank, TPBank, v.v.)
    private String gateway;

    // Ngày giờ giao dịch (yyyy-MM-dd HH:mm:ss)
    private String transactionDate;

    // Số tài khoản ngân hàng nhận
    private String accountNumber;

    // Tài khoản phụ (nếu có)
    private String subAccount;

    // Loại giao dịch: "in" (tiền vào) hoặc "out" (tiền ra)
    private String transferType;

    // Số tiền chuyển vào (VNĐ)
    private BigDecimal transferAmount;

    // Số dư lũy kế sau giao dịch
    private BigDecimal accumulated;

    // Mã code nhận diện giao dịch của SePay
    private String code;

    // Nội dung chuyển khoản ngân hàng (Nơi chứa mã đơn hàng PW-...)
    private String content;

    // Mã tham chiếu ngân hàng (FT number)
    private String referenceCode;

    // Mô tả chi tiết giao dịch
    private String description;
}
