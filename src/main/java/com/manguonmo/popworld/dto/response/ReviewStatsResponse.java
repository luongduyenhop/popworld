package com.manguonmo.popworld.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsResponse {

    private long totalReviews;
    private long pendingReviews;
    private long approvedReviews;
    private double averageRating;
}
