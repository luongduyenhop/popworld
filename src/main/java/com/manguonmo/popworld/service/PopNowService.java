package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.response.BlindBoxItemResponse;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;

import java.util.List;

public interface PopNowService {

    /**
     * Khách hàng chọn và giữ tạm một hộp trong khay (TTL 5 phút theo benchmark POP MART).
     * Đảm bảo tính nguyên tử của kho hàng và vị trí hộp.
     */
    BoxReservationResponse reserveBox(Long userId, BoxReservationRequest request);

    /**
     * Khách hàng chủ động hủy phiếu giữ hộp -> Hoàn lại tồn kho.
     */
    void cancelReservation(Long userId, String reservationCode);

    /**
     * Đánh dấu phiếu giữ hộp đã được thanh toán (chuyển sang PURCHASED).
     */
    void markPurchased(String reservationCode, String orderCode);

    /**
     * Mở hộp ngẫu nhiên (Unboxing).
     * Server-side authority: Server quyết định kết quả ngẫu nhiên.
     * Idempotency: Không tạo duplicate OwnedItem khi gọi lặp lại.
     */
    OwnedItemResponse unbox(Long userId, String reservationCode);

    /**
     * Xem túi đồ / tủ đồ ảo (Virtual Cabinet) của người dùng.
     */
    List<OwnedItemResponse> getUserCabinet(Long userId);

    /**
     * Lấy danh sách các nhân vật có thể xuất hiện trong series.
     */
    List<BlindBoxItemResponse> getSeriesItems(Long productId);

    /**
     * Quét và giải phóng các phiếu giữ hộp đã hết hạn TTL (hồi lại tồn kho).
     */
    int releaseExpiredReservations();
}
