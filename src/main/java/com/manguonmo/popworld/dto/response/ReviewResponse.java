package com.manguonmo.popworld.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;

    // Product info
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImageUrl;

    // User info
    private Long userId;
    private String userName;
    private String userEmail;

    // Review content
    private Integer rating;
    private String comment;
    private String reviewImageUrl;

    // Moderation state
    private Boolean approved;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
