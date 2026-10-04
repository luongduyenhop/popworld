package com.manguonmo.popworld.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeriesResponse {
    private Long id;
    private String name;
    private Long characterIpId;
    private String characterIpName;
    private String bannerUrl;
    private LocalDate releaseDate;
}
