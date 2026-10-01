package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.request.ShipCabinetRequest;
import com.manguonmo.popworld.dto.request.UnboxRequest;
import com.manguonmo.popworld.dto.response.*;
import com.manguonmo.popworld.entity.BoxReservation;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.entity.ReservationStatus;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.BoxReservationRepository;
import com.manguonmo.popworld.repository.OrderRepository;
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
    private final BoxReservationRepository boxReservationRepository;
    private final OrderRepository orderRepository;

    public PopNowApiController(PopNowService popNowService,
                              UserService userService,
                              OrderService orderService,
                              BoxReservationRepository boxReservationRepository,
                              OrderRepository orderRepository) {
        this.popNowService = popNowService;
        this.userService = userService;
        this.orderService = orderService;
        this.boxReservationRepository = boxReservationRepository;
        this.orderRepository = orderRepository;
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

    /**
     * Endpoint mô phỏng thanh toán VietQR thành công phục vụ Demo / Bảo vệ Đồ án
     */
    @PostMapping("/simulate-payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulatePayment(@RequestParam String orderCode,
                                                                             Principal principal) {
        User user = getAuthenticatedUser(principal);
        Order order = orderService.getOrderByCode(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderCode);
        }
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Bạn không có quyền thao tác trên đơn hàng này!");
        }

        if ("CANCELLED".equalsIgnoreCase(order.getStatus()) || "EXPIRED".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Đơn hàng này đã bị hủy hoặc hết hạn, không thể thanh toán!");
        }

        BoxReservation reservation = boxReservationRepository.findByOrderCode(orderCode).orElse(null);

        order.setStatus("PROCESSING");
        if (order.getPaidAt() == null) {
            order.setPaidAt(java.time.LocalDateTime.now());
        }
        orderRepository.save(order);

        String redirectUrl;
        if (reservation != null) {
            if (reservation.getStatus() != ReservationStatus.PURCHASED
                    && reservation.getStatus() != ReservationStatus.UNBOXED) {
                popNowService.markPurchased(reservation.getReservationCode(), orderCode);
            }
            redirectUrl = "/popnow/reveal/" + reservation.getReservationCode();
        } else {
            redirectUrl = "/checkout/success/" + orderCode;
        }

        Map<String, Object> data = Map.of(
                "orderCode", orderCode,
                "reservationCode", reservation != null ? reservation.getReservationCode() : "",
                "redirectUrl", redirectUrl
        );
        return ResponseEntity.ok(ApiResponse.success("Mô phỏng thanh toán VietQR thành công!", data));
    }

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<HintCardActionResponse>> dailyCheckIn(Principal principal) {
        User user = getAuthenticatedUser(principal);
        HintCardActionResponse response = popNowService.checkInDaily(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/shake/{reservationCode}")
    public ResponseEntity<ApiResponse<HintCardActionResponse>> shakeForHints(@PathVariable String reservationCode,
                                                                             Principal principal) {
        User user = getAuthenticatedUser(principal);
        HintCardActionResponse response = popNowService.shakeBox(user.getId(), reservationCode);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/redeem-hint-card")
    public ResponseEntity<ApiResponse<HintCardActionResponse>> redeemHintCard(
            @RequestParam(value = "packageType", defaultValue = "1") int packageType,
            Principal principal) {
        User user = getAuthenticatedUser(principal);
        HintCardActionResponse response = popNowService.redeemHintCard(user.getId(), packageType);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/use-hint-card/{reservationCode}")
    public ResponseEntity<ApiResponse<HintCardActionResponse>> useHintCard(@PathVariable String reservationCode,
                                                                           Principal principal) {
        User user = getAuthenticatedUser(principal);
        HintCardActionResponse response = popNowService.useHintCard(user.getId(), reservationCode);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @GetMapping("/user-status")
    public ResponseEntity<ApiResponse<HintCardActionResponse>> getUserStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(ApiResponse.success("Chưa đăng nhập", HintCardActionResponse.builder()
                    .luckyPoints(0).hintCards(0).canCheckInToday(false).build()));
        }
        User user = getAuthenticatedUser(principal);
        HintCardActionResponse response = popNowService.getUserPopNowStatus(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }
}
