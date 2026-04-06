package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateReportDTO {
    @NotNull(message = "Người dùng không được để trống")
    private UUID accountId;
    @NotNull(message = "Yêu cầu miêu tả báo cáo")
    private String reportDescription;
    private UUID productId;
    @NotNull(message = "Yêu cầu thêm bằng chứng")
    private List<MultipartFile> images;
}
