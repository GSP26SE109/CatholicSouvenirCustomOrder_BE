package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DashboardResponse;
import org.example.catholicsouvenircustomorder.service.DashboardService;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/admin/dashboard")
    public ResponseEntity<BaseResponse> getAdminDashboard(
            @AuthenticationPrincipal UUID adminId,
            @RequestParam Integer days) {
        DashboardResponse response = dashboardService.getAdminDashboardInDays(adminId, days);
        return ResponseEntity.ok(BaseResponse.success("Tải dashboard thành công", response));
    }

    @GetMapping("/dashboard/artisan")
    public ResponseEntity<BaseResponse> getArtisanDashboard(
            @AuthenticationPrincipal UUID artisanId,
            @RequestParam Integer days) {
        DashboardResponse response = dashboardService.getArtisanDashboardInDays(artisanId, days);
        return ResponseEntity.ok(BaseResponse.success("Tải dashboard thành công", response));
    }
}