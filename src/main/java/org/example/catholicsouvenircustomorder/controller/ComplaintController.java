package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.ComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintResponse;
import org.example.catholicsouvenircustomorder.service.ComplaintService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/complaint")
@RequiredArgsConstructor
public class ComplaintController {
    private final ComplaintService complaintService;

    @PostMapping(path = "/customer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> createComplaint(
            @AuthenticationPrincipal String customerId,
            @RequestBody ComplaintRequest complaintRequest) {
        ComplaintResponse complaintResponse = complaintService.createComplaint(UUID.fromString(customerId), complaintRequest);
        return ResponseEntity.ok(BaseResponse.success("Gửi phản hồi thành công", complaintResponse));
    }

    @PostMapping(path = "/order/{complaintId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> artisanResponse(
            @AuthenticationPrincipal String artisanId,
            @PathVariable String complaintId,
            @RequestBody @Valid ComplaintRequest complaintRequest) {
        ComplaintResponse complaintResponse = complaintService.artisanResponse(UUID.fromString(artisanId), UUID.fromString(complaintId), complaintRequest);
        return ResponseEntity.ok(BaseResponse.success("Gửi bằng chứng thành công", complaintResponse));
    }
}
