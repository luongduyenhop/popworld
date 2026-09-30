package com.manguonmo.popworld.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO phản hồi các hành động liên quan đến Hint Card và Lucky Points (Điểm danh, Lắc hộp, Đổi thẻ, Sử dụng thẻ)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HintCardActionResponse {
    private Integer luckyPoints;
    private Integer hintCards;
    private List<String> eliminatedItemNames;
    private String newlyEliminatedName;
    private Boolean canCheckInToday;
    private Integer pointsEarned;
    private String message;
    private Boolean hasUsedHintCard;
}
