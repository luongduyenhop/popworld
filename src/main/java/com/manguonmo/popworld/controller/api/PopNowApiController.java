package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.request.ShipCabinetRequest;
import com.manguonmo.popworld.dto.request.UnboxRequest;
import com.manguonmo.popworld.dto.response.*;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.OrderService;
import com.manguonmo.popworld.service.PopNowService;
import com.manguonmo.popworld.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/popnow")
public class PopNowApiController {

    private final PopNowService popNowService;
    private final UserService userService;
    private final OrderService orderService;

    public PopNowApiController(PopNowService popNowService,
                               UserService userService,
                               OrderService orderService) {
        this.popNowService = popNowService;
        this.userService = userService;
        this.orderService = orderService;
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new BadRequestException("Vui lòng đăng nhập để thao tác POP NOW.");
        }
        User user = userService.getUserByEmail(principal.getName());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadRequestException("Tài khoản không hợp lệ hoặc đã bị vô hiệu hóa.");
        }
        return user;
    }

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<BoxReservationResponse>> reserveBox(@Valid @RequestBody BoxReservationRequest request,
                                                                          Principal principal) {
        User user = getAuthenticatedUser(principal);
        BoxReservationResponse response = popNowService.reserveBox(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Giữ hộp thành công! Bạn có 5 phút để hoàn tất.", response));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@RequestParam String reservationCode,
                                                               Principal principal) {
        User user = getAuthenticatedUser(principal);
        popNowService.cancelReservation(user.getId(), reservationCode);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy giữ hộp thành công.", null));
    }

    @PostMapping("/unbox")
    public ResponseEntity<ApiResponse<OwnedItemResponse>> unbox(@Valid @RequestBody UnboxRequest request,
                                                                Principal principal) {
        User user = getAuthenticatedUser(principal);
        OwnedItemResponse response = popNowService.unbox(user.getId(), request.getReservationCode());
        return ResponseEntity.ok(ApiResponse.success("Mở hộp thành công!", response));
    }

    @GetMapping("/cabinet")
    public ResponseEntity<ApiResponse<List<OwnedItemResponse>>> getCabinet(Principal principal) {
        User user = getAuthenticatedUser(principal);
        List<OwnedItemResponse> items = popNowService.getUserCabinet(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tủ đồ thành công.", items));
    }

    @GetMapping("/series/{productId}")
    public ResponseEntity<ApiResponse<List<BlindBoxItemResponse>>> getSeriesItems(@PathVariable Long productId) {
        List<BlindBoxItemResponse> items = popNowService.getSeriesItems(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mô hình thành công.", items));
    }

    @GetMapping("/slots/{productId}")
    public ResponseEntity<ApiResponse<List<BlindBoxSlotResponse>>> getProductSlots(@PathVariable Long productId) {
        List<BlindBoxSlotResponse> slots = popNowService.getProductSlots(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách vị trí hộp thành công.", slots));
    }

    @GetMapping("/reservation/{reservationCode}")
    public ResponseEntity<ApiResponse<BoxReservationResponse>> getReservation(@PathVariable String reservationCode,
                                                                              Principal principal) {
        User user = getAuthenticatedUser(principal);
        BoxReservationResponse response = popNowService.getReservationByCode(user.getId(), reservationCode);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phiếu giữ hộp thành công.", response));
    }

    @PostMapping("/checkout/{reservationCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkoutReservation(@PathVariable String reservationCode,
                                                                                @RequestParam(required = false, defaultValue = "SEPAY") String paymentMethod,
                                                                                Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (orderService == null) {
            throw new BadRequestException("Dịch vụ tạo đơn hàng chưa sẵn sàng!");
        }
        Order order = orderService.createOrderForReservation(user.getId(), reservationCode, paymentMethod);
        Map<String, Object> data = Map.of(
                "orderCode", order.getOrderCode(),
                "totalAmount", order.getTotalAmount(),
                "paymentMethod", order.getPaymentMethod(),
                "paymentUrl", "/checkout/payment/" + order.getOrderCode()
        );
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo đơn hàng thanh toán thành công!", data));
    }

    @PostMapping("/ship")
    public ResponseEntity<ApiResponse<Map<String, Object>>> shipCabinetItems(@Valid @RequestBody ShipCabinetRequest request,
                                                                             Principal principal) {
        User user = getAuthenticatedUser(principal);
        Order order = popNowService.requestShipment(user.getId(), request);
        Map<String, Object> data = Map.of(
                "orderCode", order.getOrderCode(),
                "status", order.getStatus(),
                "recipientName", order.getRecipientName(),
                "detailedAddress", order.getDetailedAddress(),
                "redirectUrl", "/orders/" + order.getOrderCode()
        );
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu giao hàng thành công! Đơn vận đã được tạo.", data));
    }
}
