package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.template.TemplateResponse;
import org.example.catholicsouvenircustomorder.service.ProductTemplateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/artisan")
@RequiredArgsConstructor
public class ArtisanTemplateController {
    
    private final ProductTemplateService templateService;
    
    /**
     * Get artisan's own templates (Artisan only)
     * GET /api/artisan/templates
     */
    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> getArtisanTemplates(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<TemplateResponse> response = templateService.getArtisanTemplates(artisanId, pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách mẫu thành công", response));
    }
}
