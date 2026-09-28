package com.manguonmo.popworld.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimelineResponse {
    private Long id;
    private String fromStatus;
    private String toStatus;
    private String action;
    private String actor;
    private String note;
    private LocalDateTime createdAt;
}
