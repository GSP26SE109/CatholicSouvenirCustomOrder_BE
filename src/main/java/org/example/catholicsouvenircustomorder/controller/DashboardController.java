package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DashboardResponse;
import org.example.catholicsouvenircustomorder.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get admin dashboard with statistics
     * GET /api/admin/dashboard?days={days}
     */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<DashboardResponse>> getAdminDashboard(
            @AuthenticationPrincipal UUID adminId,
            @RequestParam(defaultValue = "30") Integer days) {
        
        // Validate days parameter
        if (days == null || days < 1 || days > 365) {
            return ResponseEntity.badRequest().body(
                BaseResponse.error(400, "Số ngày phải từ 1 đến 365")
            );
        }
        
        DashboardResponse response = dashboardService.getAdminDashboardInDays(adminId, days);
        return ResponseEntity.ok(BaseResponse.success("Tải dashboard thành công", response));
    }

    /**
     * Get artisan dashboard with statistics
     * GET /api/artisan/dashboard?days={days}
     */
    @GetMapping("/artisan/dashboard")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse<DashboardResponse>> getArtisanDashboard(
            @AuthenticationPrincipal UUID artisanId,
            @RequestParam(defaultValue = "30") Integer days) {
        
        // Validate days parameter
        if (days == null || days < 1 || days > 365) {
            return ResponseEntity.badRequest().body(
                BaseResponse.error(400, "Số ngày phải từ 1 đến 365")
            );
        }
        
        DashboardResponse response = dashboardService.getArtisanDashboardInDays(artisanId, days);
        return ResponseEntity.ok(BaseResponse.success("Tải dashboard thành công", response));
    }
}