package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.GenerateDesignRequest;
import org.example.catholicsouvenircustomorder.service.AIImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {
    
    private final AIImageService aiImageService;
    
    @PostMapping("/generate-design")
    public ResponseEntity<BaseResponse<String>> generateDesign(
            @Valid @RequestBody GenerateDesignRequest request) {
        
        // Build detailed prompt
        StringBuilder promptBuilder = new StringBuilder(request.getDescription());
        
        if (request.getSize() != null && !request.getSize().isEmpty()) {
            promptBuilder.append(", size: ").append(request.getSize());
        }
        
        if (request.getMaterial() != null && !request.getMaterial().isEmpty()) {
            promptBuilder.append(", material: ").append(request.getMaterial());
        }
        
        if (request.getStyle() != null && !request.getStyle().isEmpty()) {
            promptBuilder.append(", style: ").append(request.getStyle());
        }
        
        String prompt = promptBuilder.toString();
        String imageBase64 = aiImageService.generateImage(prompt);
        
        if (imageBase64 == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(BaseResponse.error(503, "Dịch vụ AI tạm thời không khả dụng. Vui lòng kiểm tra cấu hình API."));
        }
        
        return ResponseEntity.ok(BaseResponse.success("Tạo thiết kế thành công", imageBase64));
    }
}
