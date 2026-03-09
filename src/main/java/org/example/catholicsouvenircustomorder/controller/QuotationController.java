package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CreateQuotationRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateQuotationRequest;
import org.example.catholicsouvenircustomorder.dto.response.QuotationResponse;
import org.example.catholicsouvenircustomorder.service.QuotationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    @PostMapping
    public ResponseEntity<BaseResponse<QuotationResponse>> createQuotation(
            @Valid @RequestBody CreateQuotationRequest request,
            Authentication authentication) {
        
        UUID artisanId = UUID.fromString(authentication.getName());
        QuotationResponse response = quotationService.createQuotation(request, artisanId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Tạo báo giá thành công", response));
    }

    @PutMapping
    public ResponseEntity<BaseResponse<QuotationResponse>> updateQuotation(
            @Valid @RequestBody UpdateQuotationRequest request,
            Authentication authentication) {
        
        UUID artisanId = UUID.fromString(authentication.getName());
        QuotationResponse response = quotationService.updateQuotation(request, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Cập nhật báo giá thành công", response));
    }

    @PutMapping("/{quotationId}/mark-final")
    public ResponseEntity<BaseResponse<QuotationResponse>> markAsFinal(
            @PathVariable UUID quotationId,
            Authentication authentication) {
        
        UUID artisanId = UUID.fromString(authentication.getName());
        QuotationResponse response = quotationService.markAsFinal(quotationId, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Đánh dấu báo giá cuối cùng thành công", response));
    }

    @GetMapping("/request/{requestId}")
    public ResponseEntity<BaseResponse<List<QuotationResponse>>> getQuotationsByRequest(
            @PathVariable UUID requestId) {
        
        List<QuotationResponse> responses = quotationService.getQuotationsByRequest(requestId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách báo giá thành công", responses));
    }
}
