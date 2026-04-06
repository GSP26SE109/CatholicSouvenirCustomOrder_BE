package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.template.*;
import org.example.catholicsouvenircustomorder.dto.response.template.PriceBreakdownResponse;
import org.example.catholicsouvenircustomorder.dto.response.template.TemplateDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.template.TemplateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface ProductTemplateService {
    
    // CRUD operations
    TemplateResponse createTemplate(CreateTemplateRequest request, UUID artisanId);
    
    TemplateResponse updateTemplate(UUID templateId, UpdateTemplateRequest request, UUID artisanId);
    
    void deleteTemplate(UUID templateId, UUID artisanId);
    
    // Zone management
    TemplateResponse addCustomZone(UUID templateId, AddCustomZoneRequest request, UUID artisanId);
    
    TemplateResponse updateCustomZone(UUID templateId, UUID zoneId, UpdateCustomZoneRequest request, UUID artisanId);
    
    void deleteCustomZone(UUID templateId, UUID zoneId, UUID artisanId);
    
    // Query operations
    Page<TemplateResponse> getTemplatesByCategory(UUID categoryId, Pageable pageable);
    
    TemplateDetailResponse getTemplateDetail(UUID templateId);
    
    Page<TemplateResponse> getArtisanTemplates(UUID artisanId, Pageable pageable);
    
    // Price calculation
    BigDecimal calculatePrice(UUID templateId, Map<String, String> zoneInputs);
    
    PriceBreakdownResponse calculatePriceWithBreakdown(UUID templateId, Map<String, String> zoneInputs);
}
