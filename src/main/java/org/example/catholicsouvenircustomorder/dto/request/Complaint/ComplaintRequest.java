package org.example.catholicsouvenircustomorder.dto.request.Complaint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.ComplaintType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintRequest {
    private UUID orderId;
    private String message;
    private List<MultipartFile> images;
    private ComplaintType complaintType;
}
