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
@RequestMapping("/seller/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<BaseResponse> getDashboard(
            @AuthenticationPrincipal UUID artisanId,
            @RequestParam Integer days) {
        DashboardResponse response = dashboardService.getDashboardInDays(artisanId, days);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách sản phẩm thành công", response));
    }
}