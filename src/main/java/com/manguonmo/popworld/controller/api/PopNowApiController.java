package com.manguonmo.popworld.controller.api;

import com.manguonmo.popworld.dto.request.BoxReservationRequest;
import com.manguonmo.popworld.dto.request.UnboxRequest;
import com.manguonmo.popworld.dto.response.ApiResponse;
import com.manguonmo.popworld.dto.response.BlindBoxItemResponse;
import com.manguonmo.popworld.dto.response.BoxReservationResponse;
import com.manguonmo.popworld.dto.response.OwnedItemResponse;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.PopNowService;
import com.manguonmo.popworld.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/popnow")
@RequiredArgsConstructor
public class PopNowApiController {

    private final PopNowService popNowService;
    private final UserService userService;

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
}
