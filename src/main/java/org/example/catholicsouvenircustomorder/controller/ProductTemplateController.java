package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.template.AddCustomZoneRequest;
import org.example.catholicsouvenircustomorder.dto.request.template.CreateTemplateRequest;
import org.example.catholicsouvenircustomorder.dto.request.template.UpdateCustomZoneRequest;
import org.example.catholicsouvenircustomorder.dto.request.template.UpdateTemplateRequest;
import org.example.catholicsouvenircustomorder.dto.response.template.TemplateDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.template.TemplateResponse;
import org.example.catholicsouvenircustomorder.service.ProductTemplateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class ProductTemplateController {
    
    private final ProductTemplateService templateService;
    
    // ==================== ARTISAN ENDPOINTS ====================
    
    /**
     * Get my templates (Artisan only)
     * GET /api/templates/my-templates
     */
    @GetMapping("/my-templates")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> getMyTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<TemplateResponse> response = templateService.getArtisanTemplates(artisanId, pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách mẫu của tôi thành công", response));
    }
    
    /**
     * Create a new product template (Artisan only)
     * POST /api/templates
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        TemplateResponse response = templateService.createTemplate(request, artisanId);
        return ResponseEntity.ok(BaseResponse.success("Tạo mẫu sản phẩm thành công", response));
    }
    
    /**
     * Update a product template (Artisan only, ownership check)
     * PUT /api/templates/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTemplateRequest request,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        TemplateResponse response = templateService.updateTemplate(id, request, artisanId);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật mẫu sản phẩm thành công", response));
    }
    
    /**
     * Delete a product template (Artisan only)
     * DELETE /api/templates/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> deleteTemplate(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        templateService.deleteTemplate(id, artisanId);
        return ResponseEntity.ok(BaseResponse.success("Xóa mẫu sản phẩm thành công"));
    }
    
    /**
     * Add a custom zone to a template (Artisan only)
     * POST /api/templates/{id}/zones
     */
    @PostMapping("/{id}/zones")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> addCustomZone(
            @PathVariable UUID id,
            @Valid @RequestBody AddCustomZoneRequest request,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        TemplateResponse response = templateService.addCustomZone(id, request, artisanId);
        return ResponseEntity.ok(BaseResponse.success("Thêm vùng tùy chỉnh thành công", response));
    }
    
    /**
     * Update a custom zone (Artisan only, ownership check)
     * PUT /api/templates/{id}/zones/{zoneId}
     */
    @PutMapping("/{id}/zones/{zoneId}")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> updateCustomZone(
            @PathVariable UUID id,
            @PathVariable UUID zoneId,
            @Valid @RequestBody UpdateCustomZoneRequest request,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        TemplateResponse response = templateService.updateCustomZone(id, zoneId, request, artisanId);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật vùng tùy chỉnh thành công", response));
    }
    
    /**
     * Delete a custom zone (Artisan only)
     * DELETE /api/templates/{id}/zones/{zoneId}
     */
    @DeleteMapping("/{id}/zones/{zoneId}")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> deleteCustomZone(
            @PathVariable UUID id,
            @PathVariable UUID zoneId,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        templateService.deleteCustomZone(id, zoneId, artisanId);
        return ResponseEntity.ok(BaseResponse.success("Xóa vùng tùy chỉnh thành công"));
    }
    
    // ==================== PUBLIC ENDPOINTS ====================
    
    /**
     * Get all templates with optional category filter (Public)
     * GET /api/templates?categoryId={categoryId}
     */
    @GetMapping
    public ResponseEntity<BaseResponse> getTemplates(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TemplateResponse> response = templateService.getTemplatesByCategory(categoryId, pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách mẫu thành công", response));
    }
    
    /**
     * Get template detail with zones (Public)
     * GET /api/templates/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getTemplateDetail(@PathVariable UUID id) {
        TemplateDetailResponse response = templateService.getTemplateDetail(id);
        return ResponseEntity.ok(BaseResponse.success("Lấy chi tiết mẫu thành công", response));
    }
    
    /**
     * Calculate price preview for a template with zone inputs (Public)
     * GET /api/templates/{id}/price-preview
     */
    @GetMapping("/{id}/price-preview")
    public ResponseEntity<BaseResponse> getPricePreview(
            @PathVariable UUID id,
            @RequestParam Map<String, String> zoneInputs) {
        BigDecimal totalPrice = templateService.calculatePrice(id, zoneInputs);
        Map<String, Object> result = Map.of(
                "templateId", id,
                "totalPrice", totalPrice
        );
        return ResponseEntity.ok(BaseResponse.success("Tính giá thành công", result));
    }
    
    /**
     * Calculate detailed price breakdown for a template with zone inputs (Public)
     * GET /api/templates/{id}/price-breakdown
     */
    @GetMapping("/{id}/price-breakdown")
    public ResponseEntity<BaseResponse> getPriceBreakdown(
            @PathVariable UUID id,
            @RequestParam Map<String, String> zoneInputs) {
        var breakdown = templateService.calculatePriceWithBreakdown(id, zoneInputs);
        return ResponseEntity.ok(BaseResponse.success("Tính giá chi tiết thành công", breakdown));
    }
}
