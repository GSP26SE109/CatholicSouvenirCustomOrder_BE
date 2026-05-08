package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.template.*;
import org.example.catholicsouvenircustomorder.dto.response.template.*;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.ProductTemplateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductTemplateServiceImp implements ProductTemplateService {
    
    private final ProductTemplateRepository templateRepository;
    private final TemplateCustomZoneRepository zoneRepository;
    private final ArtisanRepository artisanRepository;
    private final CategoryRepository categoryRepository;
    private final CustomRequestRepository customRequestRepository;

    @Override
    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request, UUID artisanId) {
        // Validate artisan exists
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan not found"));
        
        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        // Validate basePrice > 0
        if (request.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Base price must be greater than 0");
        }
        
        // Validate at least 1 zone
        if (request.getCustomZones() == null || request.getCustomZones().isEmpty()) {
            throw new IllegalArgumentException("At least one custom zone is required");
        }
        
        // Create template
        ProductTemplate template = new ProductTemplate();
        template.setArtisan(artisan);
        template.setCategory(category);
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setBasePrice(request.getBasePrice());
        template.setMaterial(request.getMaterial());
        template.setStyle(request.getStyle());
        if (request.getBaseImages() != null) {
            template.setBaseImages(request.getBaseImages());
        }
        template.setIsActive(false); // Mặc định chờ admin duyệt
        
        ProductTemplate savedTemplate = templateRepository.save(template);
        
        // Create zones
        List<TemplateCustomZone> zones = new ArrayList<>();
        for (CreateCustomZoneRequest zoneReq : request.getCustomZones()) {
            TemplateCustomZone zone = new TemplateCustomZone();
            zone.setTemplate(savedTemplate);
            zone.setZoneName(zoneReq.getZoneName());
            zone.setZoneDescription(zoneReq.getZoneDescription());
            zone.setInputType(zoneReq.getInputType());
            zone.setInputConstraints(zoneReq.getInputConstraints());
            zone.setExtraPrice(zoneReq.getExtraPrice() != null ? zoneReq.getExtraPrice() : BigDecimal.ZERO);
            zone.setIsRequired(zoneReq.getIsRequired());
            zone.setSortOrder(zoneReq.getSortOrder());
            zones.add(zone);
        }
        
        zoneRepository.saveAll(zones);
        savedTemplate.setCustomZones(zones);
        
        return mapToTemplateResponse(savedTemplate);
    }

    @Override
    @Transactional
    public TemplateResponse updateTemplate(UUID templateId, UpdateTemplateRequest request, UUID artisanId) {
        // Find template and verify ownership
        ProductTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        if (!template.getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new IllegalArgumentException("You do not have permission to update this template");
        }
        
        // Update fields if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            template.setCategory(category);
        }
        
        if (request.getName() != null) {
            template.setName(request.getName());
        }
        
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        
        if (request.getBasePrice() != null) {
            if (request.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Base price must be greater than 0");
            }
            template.setBasePrice(request.getBasePrice());
        }
        
        if (request.getMaterial() != null) {
            template.setMaterial(request.getMaterial());
        }
        
        if (request.getStyle() != null) {
            template.setStyle(request.getStyle());
        }
        
        if (request.getBaseImages() != null) {
            template.setBaseImages(request.getBaseImages());
        }
        
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }
        
        ProductTemplate savedTemplate = templateRepository.save(template);
        
        // Update custom zones if provided
        if (request.getCustomZones() != null) {
            updateTemplateCustomZones(savedTemplate, request.getCustomZones());
        }
        
        // Reload template with updated zones
        savedTemplate = templateRepository.findById(templateId).orElseThrow();
        return mapToTemplateResponse(savedTemplate);
    }
    
    /**
     * Update custom zones for a template
     * - If zoneId is provided and exists, update that zone
     * - If zoneId is null, create new zone
     * - Delete zones that are not in the request list
     */
    private void updateTemplateCustomZones(ProductTemplate template, List<UpdateCustomZoneInTemplateRequest> zoneRequests) {
        // Get existing zones
        List<TemplateCustomZone> existingZones = zoneRepository.findByTemplate_TemplateIdOrderBySortOrder(template.getTemplateId());
        Set<UUID> requestedZoneIds = zoneRequests.stream()
                .map(UpdateCustomZoneInTemplateRequest::getZoneId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // Delete zones not in request
        List<TemplateCustomZone> zonesToDelete = existingZones.stream()
                .filter(zone -> !requestedZoneIds.contains(zone.getZoneId()))
                .collect(Collectors.toList());
        
        if (!zonesToDelete.isEmpty()) {
            zoneRepository.deleteAll(zonesToDelete);
        }
        
        // Update or create zones
        for (UpdateCustomZoneInTemplateRequest zoneReq : zoneRequests) {
            if (zoneReq.getZoneId() != null) {
                // Update existing zone
                TemplateCustomZone zone = zoneRepository.findById(zoneReq.getZoneId())
                        .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + zoneReq.getZoneId()));
                
                if (!zone.getTemplate().getTemplateId().equals(template.getTemplateId())) {
                    throw new IllegalArgumentException("Zone does not belong to this template");
                }
                
                zone.setZoneName(zoneReq.getZoneName());
                zone.setZoneDescription(zoneReq.getZoneDescription());
                zone.setInputType(zoneReq.getInputType());
                zone.setInputConstraints(zoneReq.getInputConstraints());
                zone.setExtraPrice(zoneReq.getExtraPrice() != null ? zoneReq.getExtraPrice() : BigDecimal.ZERO);
                zone.setIsRequired(zoneReq.getIsRequired());
                zone.setSortOrder(zoneReq.getSortOrder());
                
                zoneRepository.save(zone);
            } else {
                // Create new zone
                TemplateCustomZone newZone = new TemplateCustomZone();
                newZone.setTemplate(template);
                newZone.setZoneName(zoneReq.getZoneName());
                newZone.setZoneDescription(zoneReq.getZoneDescription());
                newZone.setInputType(zoneReq.getInputType());
                newZone.setInputConstraints(zoneReq.getInputConstraints());
                newZone.setExtraPrice(zoneReq.getExtraPrice() != null ? zoneReq.getExtraPrice() : BigDecimal.ZERO);
                newZone.setIsRequired(zoneReq.getIsRequired());
                newZone.setSortOrder(zoneReq.getSortOrder());
                
                zoneRepository.save(newZone);
            }
        }
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID templateId, UUID artisanId) {
        // Find template and verify ownership
        ProductTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        if (!template.getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new IllegalArgumentException("You do not have permission to delete this template");
        }
        
        // NOTE: Template deletion check removed
        // Template-based flow now uses Order entity, not CustomRequest
        // CustomRequest is only for request-based flow (no template reference)
        
        templateRepository.delete(template);
    }

    @Override
    public Page<TemplateResponse> getTemplatesByCategory(UUID categoryId, Pageable pageable) {
        Page<ProductTemplate> templates;
        if (categoryId != null) {
            templates = templateRepository.findByCategory_CategoryIdAndIsActiveTrue(categoryId, pageable);
        } else {
            templates = templateRepository.findByIsActiveTrue(pageable);
        }
        return templates.map(this::mapToTemplateResponse);
    }

    @Override
    public TemplateDetailResponse getTemplateDetail(UUID templateId) {
        ProductTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        return mapToTemplateDetailResponse(template);
    }

    @Override
    public Page<TemplateResponse> getArtisanTemplates(UUID artisanId, Pageable pageable) {
        Page<ProductTemplate> templates = templateRepository.findByArtisan_ArtisanUuidAndIsActiveTrue(artisanId, pageable);
        return templates.map(this::mapToTemplateResponse);
    }

    @Override
    @Transactional
    public TemplateResponse addCustomZone(UUID templateId, AddCustomZoneRequest request, UUID artisanId) {
        // Find template and verify ownership
        ProductTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        if (!template.getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new IllegalArgumentException("You do not have permission to modify this template");
        }
        
        // Check sortOrder uniqueness
        if (zoneRepository.existsByTemplateAndSortOrder(template, request.getSortOrder())) {
            throw new IllegalArgumentException("Sort order " + request.getSortOrder() + " already exists for this template");
        }
        
        // Create zone
        TemplateCustomZone zone = new TemplateCustomZone();
        zone.setTemplate(template);
        zone.setZoneName(request.getZoneName());
        zone.setZoneDescription(request.getZoneDescription());
        zone.setInputType(request.getInputType());
        zone.setInputConstraints(request.getInputConstraints());
        zone.setExtraPrice(request.getExtraPrice() != null ? request.getExtraPrice() : BigDecimal.ZERO);
        zone.setIsRequired(request.getIsRequired());
        zone.setSortOrder(request.getSortOrder());
        
        zoneRepository.save(zone);
        
        // Reload template with zones
        template = templateRepository.findById(templateId).orElseThrow();
        return mapToTemplateResponse(template);
    }

    @Override
    @Transactional
    public TemplateResponse updateCustomZone(UUID templateId, UUID zoneId, UpdateCustomZoneRequest request, UUID artisanId) {
        // Find template and verify ownership
        ProductTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        if (!template.getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new IllegalArgumentException("You do not have permission to modify this template");
        }
        
        // Find zone
        TemplateCustomZone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
        
        if (!zone.getTemplate().getTemplateId().equals(templateId)) {
            throw new IllegalArgumentException("Zone does not belong to this template");
        }
        
        // Update fields if provided
        if (request.getZoneName() != null) {
            zone.setZoneName(request.getZoneName());
        }
        
        if (request.getZoneDescription() != null) {
            zone.setZoneDescription(request.getZoneDescription());
        }
        
        if (request.getInputType() != null) {
            zone.setInputType(request.getInputType());
        }
        
        if (request.getInputConstraints() != null) {
            zone.setInputConstraints(request.getInputConstraints());
        }
        
        if (request.getExtraPrice() != null) {
            zone.setExtraPrice(request.getExtraPrice());
        }
        
        if (request.getIsRequired() != null) {
            zone.setIsRequired(request.getIsRequired());
        }
        
        if (request.getSortOrder() != null) {
            // Check if new sortOrder conflicts with another zone
            Optional<TemplateCustomZone> existingZone = zoneRepository.findByTemplateAndSortOrder(template, request.getSortOrder());
            if (existingZone.isPresent() && !existingZone.get().getZoneId().equals(zoneId)) {
                throw new IllegalArgumentException("Sort order " + request.getSortOrder() + " already exists for this template");
            }
            zone.setSortOrder(request.getSortOrder());
        }
        
        zoneRepository.save(zone);
        
        // Reload template with zones
        template = templateRepository.findById(templateId).orElseThrow();
        return mapToTemplateResponse(template);
    }

    @Override
    @Transactional
    public void deleteCustomZone(UUID templateId, UUID zoneId, UUID artisanId) {
        // Find template and verify ownership
        ProductTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        if (!template.getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new IllegalArgumentException("You do not have permission to modify this template");
        }
        
        // Find zone
        TemplateCustomZone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
        
        if (!zone.getTemplate().getTemplateId().equals(templateId)) {
            throw new IllegalArgumentException("Zone does not belong to this template");
        }
        
        // Check if there are active requests using this zone
        // NOTE: Zone deletion check removed
        // Template-based flow now uses Order entity, not CustomRequest
        // CustomRequest is only for request-based flow (no template/customization reference)
        
        zoneRepository.delete(zone);
    }

    @Override
    public BigDecimal calculatePrice(UUID templateId, Map<String, String> zoneInputs) {
        ProductTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        BigDecimal totalPrice = template.getBasePrice();
        
        // Only process if zoneInputs has actual values
        if (zoneInputs != null && !zoneInputs.isEmpty()) {
            List<TemplateCustomZone> zones = zoneRepository.findByTemplate_TemplateIdOrderBySortOrder(templateId);
            
            for (TemplateCustomZone zone : zones) {
                String zoneIdStr = zone.getZoneId().toString();
                String inputValue = zoneInputs.get(zoneIdStr);
                
                // Only process if input value exists and is not empty/blank
                if (inputValue != null && !inputValue.trim().isEmpty()) {
                    // Validate zone input against constraints
                    validateZoneInput(zone, inputValue);
                    
                    // Add extra price for filled zones
                    if (zone.getExtraPrice() != null) {
                        totalPrice = totalPrice.add(zone.getExtraPrice());
                    }
                }
            }
        }
        
        return totalPrice;
    }
    
    @Override
    public PriceBreakdownResponse calculatePriceWithBreakdown(UUID templateId, Map<String, String> zoneInputs) {
        ProductTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        BigDecimal totalPrice = template.getBasePrice();
        List<ZonePriceDetail> zonePrices = new ArrayList<>();
        
        // Only process if zoneInputs has actual values
        if (zoneInputs != null && !zoneInputs.isEmpty()) {
            List<TemplateCustomZone> zones = zoneRepository.findByTemplate_TemplateIdOrderBySortOrder(templateId);
            
            for (TemplateCustomZone zone : zones) {
                String zoneIdStr = zone.getZoneId().toString();
                String inputValue = zoneInputs.get(zoneIdStr);
                
                // Only process if input value exists and is not empty/blank
                if (inputValue != null && !inputValue.trim().isEmpty()) {
                    // Validate zone input against constraints
                    validateZoneInput(zone, inputValue);
                    
                    // Add extra price for filled zones
                    if (zone.getExtraPrice() != null && zone.getExtraPrice().compareTo(BigDecimal.ZERO) > 0) {
                        totalPrice = totalPrice.add(zone.getExtraPrice());
                        zonePrices.add(new ZonePriceDetail(
                            zone.getZoneId(),
                            zone.getZoneName(),
                            zone.getExtraPrice()
                        ));
                    }
                }
            }
        }
        
        return new PriceBreakdownResponse(template.getBasePrice(), zonePrices, totalPrice);
    }
    
    // Helper method to validate zone input
    private void validateZoneInput(TemplateCustomZone zone, String input) {
        if (zone.getInputConstraints() == null || zone.getInputConstraints().isEmpty()) {
            return;
        }
        
        switch (zone.getInputType()) {
            case TEXT:
                if (zone.getInputConstraints().containsKey("maxChars")) {
                    Integer maxChars = (Integer) zone.getInputConstraints().get("maxChars");
                    if (input.length() > maxChars) {
                        throw new IllegalArgumentException("Input for zone '" + zone.getZoneName() + "' exceeds maximum length of " + maxChars);
                    }
                }
                break;
                
            case COLOR:
                if (zone.getInputConstraints().containsKey("allowedColors")) {
                    @SuppressWarnings("unchecked")
                    List<String> allowedColors = (List<String>) zone.getInputConstraints().get("allowedColors");
                    if (!allowedColors.contains(input)) {
                        throw new IllegalArgumentException("Color '" + input + "' is not allowed for zone '" + zone.getZoneName() + "'");
                    }
                }
                break;
                
            case SELECT:
                if (zone.getInputConstraints().containsKey("options")) {
                    @SuppressWarnings("unchecked")
                    List<String> options = (List<String>) zone.getInputConstraints().get("options");
                    if (!options.contains(input)) {
                        throw new IllegalArgumentException("Option '" + input + "' is not valid for zone '" + zone.getZoneName() + "'");
                    }
                }
                break;
                
            case IMAGE:
                // Image validation can be added here if needed
                break;
        }
    }
    
    // Mapping methods
    private TemplateResponse mapToTemplateResponse(ProductTemplate template) {
        TemplateResponse response = new TemplateResponse();
        response.setTemplateId(template.getTemplateId());
        response.setArtisanId(template.getArtisan().getArtisanUuid());
        response.setArtisanName(template.getArtisan().getArtisanName());
        response.setCategoryId(template.getCategory() != null ? template.getCategory().getCategoryId() : null);
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setBasePrice(template.getBasePrice());
        response.setMaterial(template.getMaterial());
        response.setStyle(template.getStyle());
        response.setBaseImages(template.getBaseImages());
        response.setIsActive(template.getIsActive());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        response.setZoneCount(template.getCustomZones() != null ? template.getCustomZones().size() : 0);
        return response;
    }
    
    private TemplateDetailResponse mapToTemplateDetailResponse(ProductTemplate template) {
        TemplateDetailResponse response = new TemplateDetailResponse();
        response.setTemplateId(template.getTemplateId());
        response.setArtisanId(template.getArtisan().getArtisanUuid());
        response.setArtisanName(template.getArtisan().getArtisanName());
        response.setCategoryId(template.getCategory() != null ? template.getCategory().getCategoryId() : null);
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setBasePrice(template.getBasePrice());
        response.setMaterial(template.getMaterial());
        response.setStyle(template.getStyle());
        response.setBaseImages(template.getBaseImages());
        response.setIsActive(template.getIsActive());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        
        // Map zones
        List<ZoneResponse> zones = template.getCustomZones().stream()
                .map(this::mapToZoneResponse)
                .collect(Collectors.toList());
        response.setCustomZones(zones);
        
        return response;
    }
    
    private ZoneResponse mapToZoneResponse(TemplateCustomZone zone) {
        ZoneResponse response = new ZoneResponse();
        response.setZoneId(zone.getZoneId());
        response.setZoneName(zone.getZoneName());
        response.setZoneDescription(zone.getZoneDescription());
        response.setInputType(zone.getInputType());
        response.setInputConstraints(zone.getInputConstraints());
        response.setExtraPrice(zone.getExtraPrice());
        response.setIsRequired(zone.getIsRequired());
        response.setSortOrder(zone.getSortOrder());
        return response;
    }
    
    // ==================== ADMIN OPERATIONS ====================
    
    @Override
    @Transactional
    public TemplateResponse approveTemplate(UUID templateId) {
        ProductTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template không tồn tại"));
        
        template.setIsActive(true);
        template.setUpdatedAt(java.time.LocalDateTime.now());
        ProductTemplate savedTemplate = templateRepository.save(template);
        
        return mapToTemplateResponse(savedTemplate);
    }
    
    @Override
    @Transactional
    public TemplateResponse rejectTemplate(UUID templateId) {
        ProductTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template không tồn tại"));
        
        template.setIsActive(false);
        template.setUpdatedAt(java.time.LocalDateTime.now());
        ProductTemplate savedTemplate = templateRepository.save(template);
        
        return mapToTemplateResponse(savedTemplate);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TemplateResponse> getPendingTemplates(Pageable pageable) {
        Page<ProductTemplate> templates = templateRepository.findByIsActiveFalse(pageable);
        return templates.map(this::mapToTemplateResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TemplateResponse> getApprovedTemplates(Pageable pageable) {
        Page<ProductTemplate> templates = templateRepository.findByIsActiveTrue(pageable);
        return templates.map(this::mapToTemplateResponse);
    }
}
