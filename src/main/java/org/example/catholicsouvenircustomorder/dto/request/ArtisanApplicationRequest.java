package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtisanApplicationRequest {
    @NotBlank(message = "Tên nghệ nhân không được để trống")
    private String artisanName;

    @NotBlank(message = "Giới thiệu không được để trống")
    @Size(max = 1000, message = "Giới thiệu không được vượt quá 1000 ký tự")
    private String bio;

    @NotNull(message = "Số năm kinh nghiệm không được để trống")
    @Min(value = 0, message = "Số năm kinh nghiệm phải lớn hơn hoặc bằng 0")
    private Integer experienceYear;

    private String portfolioUrl;

    @NotBlank(message = "Chuyên môn không được để trống")
    private String specialization;
}
