package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.UpdateArtisanProfileRequest;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DashboardResponse;
import org.example.catholicsouvenircustomorder.dto.response.account.ArtisanResponseDTO;
import org.example.catholicsouvenircustomorder.service.ArtisanService;
import org.example.catholicsouvenircustomorder.service.DashboardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/artisans")
@RequiredArgsConstructor
public class ArtisanController {

    private final ArtisanService artisanService;
    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<BaseResponse<Page<ArtisanResponseDTO>>> getAllArtisans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

//        Sort sort = sortDir.equalsIgnoreCase("desc")
//                ? Sort.by(sortBy).descending()
//                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size);
        Page<ArtisanResponseDTO> artisans = artisanService.getAllArtisans(pageable);

        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách thợ thủ công thành công", artisans));
    }

    @GetMapping("/{artisanId}")
    public ResponseEntity<BaseResponse<ArtisanResponseDTO>> getArtisanById(
            @PathVariable UUID artisanId) {

        ArtisanResponseDTO artisan = artisanService.getArtisanById(artisanId);

        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin thợ thủ công thành công", artisan));
    }

    @GetMapping("/profile")
    public ResponseEntity<BaseResponse<ArtisanResponseDTO>> getArtisanProfile(
            @AuthenticationPrincipal UUID artisanId) {

        ArtisanResponseDTO artisan = artisanService.getArtisanProfile(artisanId);

        return ResponseEntity.ok(BaseResponse.success("Lấy hồ sơ cá nhân thành công", artisan));
    }

    @PutMapping("/profile")
    public ResponseEntity<BaseResponse<ArtisanResponseDTO>> updateArtisanProfile(
            @AuthenticationPrincipal UUID artisanId,
            @RequestBody UpdateArtisanProfileRequest request) {

        ArtisanResponseDTO artisan = artisanService.updateArtisanProfile(artisanId, request);

        return ResponseEntity.ok(BaseResponse.success("Cập nhật hồ sơ cá nhân thành công", artisan));
    }

//    @GetMapping("/dashboard")
//    public ResponseEntity<BaseResponse> getDashboard(
//            @AuthenticationPrincipal UUID artisanId,
//            @RequestParam Integer days
//    ){
//        DashboardResponse response = dashboardService.getArtisanDashboardInDays(artisanId,days);
//        return ResponseEntity.ok(BaseResponse.success("Tải dashboard thành công", response));
//    }
}
