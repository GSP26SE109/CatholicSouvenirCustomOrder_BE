package org.example.catholicsouvenircustomorder.service.imp;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.CreateReportDTO;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.example.catholicsouvenircustomorder.model.Report;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ReportRepository;
import org.example.catholicsouvenircustomorder.service.ProductImageService;
import org.example.catholicsouvenircustomorder.service.ReportService;
import org.springframework.stereotype.Service;
import org.example.catholicsouvenircustomorder.dto.response.ReportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImp implements ReportService {
    private final ReportRepository reportRepository;
    private final AccountRepository accountRepository;
    private final Cloudinary cloudinary;

    @Override
    public ReportResponse findById(UUID id) {
        Report report = reportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Report không tồn tại"));
        return convertToResponseDTO(report);
    }

    @Override
    public List<ReportResponse> findByAccountId(UUID accountId) {
        List<Report> reports = reportRepository.findByAccount_AccountId(accountId);
        return reports.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ReportResponse createReport(CreateReportDTO dto) {
        Report report = new Report();
        report.setAccount(accountRepository.findById(dto.getAccountId()).orElseThrow(() -> new RuntimeException("Account not found")));
        report.setReportDescription(dto.getReportDescription());
        List<String> evidences = new ArrayList<>();
        for (MultipartFile image : dto.getImages()) {
            if (!image.isEmpty()) {
                try {
                    Map uploadResult = cloudinary.uploader().upload(
                            image.getBytes(),
                            ObjectUtils.emptyMap()
                    );

                    String imageUrl = uploadResult.get("secure_url").toString();
                    evidences.add(imageUrl);
                } catch (IOException e) {
                    throw new RuntimeException("Upload hình ảnh thất bại");
                }
            }
        }
        report.setImage_url(evidences);
        return convertToResponseDTO(reportRepository.save(report));
    }

    private ReportResponse convertToResponseDTO(Report report) {
        return ReportResponse.builder()
                .accountId(report.getAccount().getAccountId())
                .accountName(report.getAccount().getFullName())
                .reportDescription(report.getReportDescription())
                .evidences(report.getImage_url())
                .build();
    }
}
