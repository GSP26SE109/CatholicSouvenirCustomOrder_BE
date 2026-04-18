package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateArtisanProfileRequest {
    
    @Size(max = 200, message = "Tên thợ thủ công không được vượt quá 200 ký tự")
    private String artisanName;
    
    @Size(max = 2000, message = "Bio không được vượt quá 2000 ký tự")
    private String bio;
    
    @Min(value = 0, message = "Số năm kinh nghiệm phải lớn hơn hoặc bằng 0")
    private Integer experienceYears;
    
    @Size(max = 500, message = "URL portfolio không được vượt quá 500 ký tự")
    private String portfolioUrl;
    
    @Size(max = 200, message = "Chuyên môn không được vượt quá 200 ký tự")
    private String specialization;
}
