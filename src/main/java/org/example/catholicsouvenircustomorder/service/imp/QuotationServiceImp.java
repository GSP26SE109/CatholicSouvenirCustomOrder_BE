//package org.example.catholicsouvenircustomorder.service.imp;
//
//import lombok.RequiredArgsConstructor;
//import org.example.catholicsouvenircustomorder.dto.request.CreateQuotationRequest;
//import org.example.catholicsouvenircustomorder.dto.request.UpdateQuotationRequest;
//import org.example.catholicsouvenircustomorder.dto.response.QuotationResponse;
//import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
//import org.example.catholicsouvenircustomorder.model.*;
//import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
//import org.example.catholicsouvenircustomorder.repository.CustomRequestRepository;
//import org.example.catholicsouvenircustomorder.repository.QuotationRepository;
//import org.example.catholicsouvenircustomorder.service.QuotationService;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class QuotationServiceImp implements QuotationService {
//
//    private final QuotationRepository quotationRepository;
//    private final CustomRequestRepository customRequestRepository;
//    private final ArtisanRepository artisanRepository;
//    private final SimpMessagingTemplate messagingTemplate;
//
//    @Override
//    @Transactional
//    public QuotationResponse createQuotation(CreateQuotationRequest request, UUID artisanId) {
//        CustomRequest customRequest = customRequestRepository.findById(request.getRequestId())
//                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tùy chỉnh"));
//
//        Artisan artisan = artisanRepository.findById(artisanId)
//                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));
//
//        Quotation quotation = new Quotation();
//        quotation.setCustomRequest(customRequest);
//        quotation.setArtisan(artisan);
//        quotation.setPrice(request.getPrice());
//        quotation.setNotes(request.getNotes());
//        quotation.setStatus(QuotationStatus.SENT);
//
//        quotation = quotationRepository.save(quotation);
//
//        // Notify customer via WebSocket
//        QuotationResponse response = mapToResponse(quotation);
//        messagingTemplate.convertAndSend(
//            "/topic/request/" + customRequest.getRequestId() + "/quotations",
//            response
//        );
//
//        return response;
//    }
//
//    @Override
//    @Transactional
//    public QuotationResponse updateQuotation(UpdateQuotationRequest request, UUID artisanId) {
//        Quotation quotation = quotationRepository.findById(request.getQuotationId())
//                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá"));
//
//        if (!quotation.getArtisan().getArtisanUuid().equals(artisanId)) {
//            throw new IllegalArgumentException("Bạn không có quyền cập nhật báo giá này");
//        }
//
//        quotation.setPrice(request.getPrice());
//        quotation.setNotes(request.getNotes());
//        quotation.setStatus(QuotationStatus.REVISED);
//        quotation.setVersion(quotation.getVersion() + 1);
//
//        quotation = quotationRepository.save(quotation);
//
//        // Notify customer via WebSocket
//        QuotationResponse response = mapToResponse(quotation);
//        messagingTemplate.convertAndSend(
//            "/topic/request/" + quotation.getCustomRequest().getRequestId() + "/quotations",
//            response
//        );
//
//        return response;
//    }
//
//    @Override
//    @Transactional
//    public QuotationResponse markAsFinal(UUID quotationId, UUID artisanId) {
//        Quotation quotation = quotationRepository.findById(quotationId)
//                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá"));
//
//        if (!quotation.getArtisan().getArtisanUuid().equals(artisanId)) {
//            throw new IllegalArgumentException("Bạn không có quyền cập nhật báo giá này");
//        }
//
//        quotation.setStatus(QuotationStatus.FINAL);
//        quotation = quotationRepository.save(quotation);
//
//        return mapToResponse(quotation);
//    }
//
//    @Override
//    public List<QuotationResponse> getQuotationsByRequest(UUID requestId) {
//        return quotationRepository.findByCustomRequest_RequestId(requestId)
//                .stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    private QuotationResponse mapToResponse(Quotation quotation) {
//        QuotationResponse response = new QuotationResponse();
//        response.setQuotationId(quotation.getQuotationId());
//        response.setRequestId(quotation.getCustomRequest().getRequestId());
//        response.setArtisanId(quotation.getArtisan().getArtisanUuid());
//        response.setArtisanName(quotation.getArtisan().getArtisanName());
//        response.setPrice(quotation.getPrice());
//        response.setNotes(quotation.getNotes());
//        response.setStatus(quotation.getStatus());
//        response.setVersion(quotation.getVersion());
//        response.setCreatedAt(quotation.getCreatedAt());
//        response.setUpdatedAt(quotation.getUpdatedAt());
//        return response;
//    }
//}
