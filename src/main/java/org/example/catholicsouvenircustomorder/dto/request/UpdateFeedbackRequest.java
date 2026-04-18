package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateFeedbackRequest {
    
    @NotNull(message = "Đánh giá không được để trống")
    @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa là 5 sao")
    private Integer rating;
    
    @Size(max = 1000, message = "Nhận xét không được vượt quá 1000 ký tự")
    private String comment;
}
