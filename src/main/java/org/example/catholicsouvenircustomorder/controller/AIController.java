package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.AIPromptRequest;
import org.example.catholicsouvenircustomorder.dto.request.GenerateConceptImageRequest;
import org.example.catholicsouvenircustomorder.dto.request.GenerateDesignRequest;
import org.example.catholicsouvenircustomorder.dto.request.ScriptureRecommendRequest;
import org.example.catholicsouvenircustomorder.dto.response.AIImageResponse;
import org.example.catholicsouvenircustomorder.dto.response.ScriptureRecommendResponse;
import org.example.catholicsouvenircustomorder.service.AIImageService;
import org.example.catholicsouvenircustomorder.service.AIScriptureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {
    
    private final AIImageService aiImageService;
    private final AIScriptureService aiScriptureService;
    
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
        String imageUrl = aiImageService.generateImage(prompt);
        
        if (imageUrl == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(BaseResponse.error(503, "Dịch vụ AI tạm thời không khả dụng. Vui lòng kiểm tra cấu hình API hoặc thử lại sau."));
        }
        
        // Check if it's a placeholder image
        if (imageUrl.startsWith("data:image/svg+xml")) {
            return ResponseEntity.ok(BaseResponse.success(
                "Dịch vụ AI đang bảo trì. Đã tạo ảnh placeholder tạm thời.", 
                imageUrl
            ));
        }
        
        return ResponseEntity.ok(BaseResponse.success("Tạo thiết kế thành công", imageUrl));
    }
    
    @PostMapping("/generate-concept")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<AIImageResponse>> generateConceptImage(
            @Valid @RequestBody GenerateConceptImageRequest request) {
        
        // Build AIPromptRequest with the description
        AIPromptRequest aiPromptRequest = AIPromptRequest.builder()
                .additionalDescription(request.getDescription())
                .build();
        
        // Use existing service method
        AIImageResponse response = aiImageService.generateConceptImage(aiPromptRequest);
        
        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(BaseResponse.error(503, response.getErrorMessage() != null ? 
                    response.getErrorMessage() : "Dịch vụ AI tạm thời không khả dụng. Vui lòng thử lại sau."));
        }
        
        return ResponseEntity.ok(BaseResponse.success("Tạo ảnh concept thành công", response));
    }
    
    /**
     * AI Scripture Recommender
     * Recommend Bible verses for engraving on Catholic souvenir items
     * POST /api/ai/recommend-scripture
     */
    @PostMapping("/recommend-scripture")
    public ResponseEntity<BaseResponse<ScriptureRecommendResponse>> recommendScripture(
            @Valid @RequestBody ScriptureRecommendRequest request) {
        
        ScriptureRecommendResponse response = aiScriptureService.recommendScriptures(request);
        
        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(BaseResponse.error(503, response.getErrorMessage() != null ? 
                    response.getErrorMessage() : "Không thể gợi ý câu Kinh Thánh. Vui lòng thử lại sau."));
        }
        
        return ResponseEntity.ok(BaseResponse.success(response.getMessage(), response));
    }
    
    /**
     * Get popular scriptures for a specific occasion
     * GET /api/ai/popular-scriptures?occasion={occasion}&language={language}
     */
    @GetMapping("/popular-scriptures")
    public ResponseEntity<BaseResponse<ScriptureRecommendResponse>> getPopularScriptures(
            @RequestParam String occasion,
            @RequestParam(defaultValue = "en") String language) {
        
        ScriptureRecommendResponse response = aiScriptureService.getPopularScripturesForOccasion(occasion, language);
        
        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(BaseResponse.error(503, response.getErrorMessage() != null ? 
                    response.getErrorMessage() : "Không thể lấy câu Kinh Thánh. Vui lòng thử lại sau."));
        }
        
        return ResponseEntity.ok(BaseResponse.success(response.getMessage(), response));
    }
}

