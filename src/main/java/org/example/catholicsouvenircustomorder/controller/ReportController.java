package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CreateReportDTO;
import org.example.catholicsouvenircustomorder.dto.response.ReportResponse;
import org.example.catholicsouvenircustomorder.service.ReportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/{reportId}")
    public ResponseEntity<BaseResponse> getById(@RequestParam UUID reportId) {
        ReportResponse report = reportService.findById(reportId);
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin report thành công", report));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<BaseResponse> getByAccountId(@RequestParam UUID accountId) {
        List<ReportResponse> reports = reportService.findByAccountId(accountId);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách report thành công", reports));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> createReport(@Valid @ModelAttribute CreateReportDTO dto) {
        ReportResponse report = reportService.createReport(dto);
        return ResponseEntity.ok(BaseResponse.success("Tạo report thành công", report));
    }
}
