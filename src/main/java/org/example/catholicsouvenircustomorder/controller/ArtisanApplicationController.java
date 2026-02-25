package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.ArtisanApplicationRequest;
import org.example.catholicsouvenircustomorder.dto.request.RegisterArtisanRequest;
import org.example.catholicsouvenircustomorder.dto.request.ReviewApplicationRequest;
import org.example.catholicsouvenircustomorder.dto.response.ArtisanApplicationResponse;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.service.ArtisanApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/artisan-applications")
@RequiredArgsConstructor
public class ArtisanApplicationController {

    private final ArtisanApplicationService artisanApplicationService;

    /**
     * Đăng ký artisan cho người mới (không cần đăng nhập)
     */
    @PostMapping("/register")
    public ResponseEntity<BaseResponse> registerAsArtisan(@Valid @RequestBody RegisterArtisanRequest request) {
        try {
            ArtisanApplicationResponse response = artisanApplicationService.registerAsArtisan(request);
            return ResponseEntity.ok(BaseResponse.success(response.getMessage(), response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.error(400, e.getMessage()));
        }
    }

    /**
     * Customer nộp đơn artisan (cần đăng nhập với role CUSTOMER)
     */
    @PostMapping("/submit")
    @PostAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> submitApplication(
            @Valid @RequestBody ArtisanApplicationRequest request,
            @AuthenticationPrincipal UUID accountId) {
        try {
            ArtisanApplicationResponse response = artisanApplicationService.submitApplication(request, accountId);
            return ResponseEntity.ok(BaseResponse.success(response.getMessage(), response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.error(400, e.getMessage()));
        }
    }

    /**
     * Xem danh sách đơn đăng ký của mình
     */
    @GetMapping("/my-applications")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getMyApplications(@AuthenticationPrincipal UUID accountId) {
        List<ArtisanApplicationResponse> applications = artisanApplicationService.getMyApplications(accountId);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách đơn đăng ký thành công", applications));
    }

    /**
     * Xem chi tiết đơn đăng ký
     */
    @GetMapping("/{applicationId}")
    public ResponseEntity<BaseResponse> getApplicationById(@PathVariable UUID applicationId) {
        try {
            ArtisanApplicationResponse response = artisanApplicationService.getApplicationById(applicationId);
            return ResponseEntity.ok(BaseResponse.success("Lấy thông tin đơn đăng ký thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.error(400, e.getMessage()));
        }
    }

    /**
     * Admin xem danh sách đơn chờ duyệt
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse> getPendingApplications() {
        List<ArtisanApplicationResponse> applications = artisanApplicationService.getPendingApplications();
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách đơn chờ duyệt thành công", applications));
    }

    /**
     * Admin phê duyệt/từ chối đơn
     */
    @PutMapping("/{applicationId}/review")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse> reviewApplication(
            @PathVariable UUID applicationId,
            @Valid @RequestBody ReviewApplicationRequest request,
            @AuthenticationPrincipal Account admin) {
        try {
            ArtisanApplicationResponse response = artisanApplicationService.reviewApplication(applicationId, request, admin);
            return ResponseEntity.ok(BaseResponse.success(response.getMessage(), response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.error(400, e.getMessage()));
        }
    }
}
