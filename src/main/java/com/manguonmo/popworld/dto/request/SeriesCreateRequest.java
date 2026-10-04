package com.manguonmo.popworld.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeriesCreateRequest {

    @NotBlank(message = "Tên Series / Bộ sưu tập không được để trống!")
    @Size(max = 150, message = "Tên Series không được vượt quá 150 ký tự!")
    private String name;

    @NotNull(message = "Vui lòng chọn Nhân vật / IP đại diện!")
    private Long characterIpId;

    private String description;

    private String bannerUrl;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate releaseDate;
}
