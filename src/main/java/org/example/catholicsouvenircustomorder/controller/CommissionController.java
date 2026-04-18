package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.UpdateCommissionRateRequest;
import org.example.catholicsouvenircustomorder.dto.response.CommissionConfigResponse;
import org.example.catholicsouvenircustomorder.dto.response.CommissionRateResponse;
import org.example.catholicsouvenircustomorder.dto.response.CommissionReportResponse;
import org.example.catholicsouvenircustomorder.model.SystemConfig;
import org.example.catholicsouvenircustomorder.service.CommissionReportService;
import org.example.catholicsouvenircustomorder.service.SystemConfigService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for commission management endpoints
 * Handles commission rate configuration and reporting
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommissionController {
    
    private final SystemConfigService systemConfigService;
    private final CommissionReportService commissionReportService;
    
    /**
     * Get current commission rate (Public endpoint for authenticated users)
     * Requirements: 10.1, 10.2
     */
    @GetMapping("/commission/rate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<CommissionRateResponse>> getCurrentRate() {
        log.info("GET /api/commission/rate - Getting current commission rate");
        
        BigDecimal rate = systemConfigService.getCommissionRate();
        
        CommissionRateResponse response = CommissionRateResponse.builder()
            .commissionRate(rate)
            .description("Tỷ lệ phí sàn hiện tại")
            .build();
        
        return ResponseEntity.ok(BaseResponse.success("Lấy commission rate thành công", response));
    }
    
    /**
     * Get commission configuration details (Admin only)
     * Requirements: 1.4, 2.1
     */
    @GetMapping("/admin/commission/config")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<CommissionConfigResponse>> getConfig() {
        log.info("GET /api/admin/commission/config - Getting commission configuration");
        
        SystemConfig config = systemConfigService.getConfig("PLATFORM_COMMISSION_RATE");
        
        CommissionConfigResponse response = CommissionConfigResponse.builder()
            .commissionRate(new BigDecimal(config.getConfigValue()))
            .updatedAt(config.getUpdatedAt())
            .updatedBy(config.getUpdatedBy() != null ? config.getUpdatedBy().getEmail() : null)
            .allowedUpdateTime("00:00 - 00:59")
            .build();
        
        return ResponseEntity.ok(BaseResponse.success("Lấy cấu hình thành công", response));
    }
    
    /**
     * Update commission rate (Admin only)
     * Requirements: 1.3, 2.2, 2.3, 2.4, 2.5, 2.6
     */
    @PutMapping("/admin/commission/config")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<CommissionConfigResponse>> updateConfig(
            @Valid @RequestBody UpdateCommissionRateRequest request,
            @AuthenticationPrincipal UUID adminId) {
        
        log.info("PUT /api/admin/commission/config - Updating commission rate to {} by admin {}", 
            request.getCommissionRate(), adminId);
        
        // Update commission rate (includes time window validation, rate validation, and cache clearing)
        systemConfigService.updateCommissionRate(request.getCommissionRate(), adminId);
        
        // Get updated config
        SystemConfig config = systemConfigService.getConfig("PLATFORM_COMMISSION_RATE");
        CommissionConfigResponse response = CommissionConfigResponse.builder()
            .commissionRate(new BigDecimal(config.getConfigValue()))
            .updatedAt(config.getUpdatedAt())
            .updatedBy(config.getUpdatedBy().getEmail())
            .allowedUpdateTime("00:00 - 00:59")
            .build();
        
        return ResponseEntity.ok(BaseResponse.success("Cập nhật commission rate thành công", response));
    }
    
    /**
     * Get commission report (Admin only)
     * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
     */
    @GetMapping("/admin/commission/report")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<CommissionReportResponse>> getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String groupBy) {
        
        log.info("GET /api/admin/commission/report - Generating report from {} to {} grouped by {}", 
            startDate, endDate, groupBy);
        
        CommissionReportService.GroupBy groupByEnum;
        try {
            groupByEnum = CommissionReportService.GroupBy.valueOf(groupBy.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid groupBy parameter: {}. Using DAY as default", groupBy);
            groupByEnum = CommissionReportService.GroupBy.DAY;
        }
        
        CommissionReportResponse report = commissionReportService.generateReport(
            startDate, endDate, groupByEnum
        );
        
        return ResponseEntity.ok(BaseResponse.success("Tạo báo cáo thành công", report));
    }
}
