package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreateReportDTO;
import org.example.catholicsouvenircustomorder.model.Report;
import org.example.catholicsouvenircustomorder.dto.response.ReportResponse;

import java.util.List;
import java.util.UUID;

public interface ReportService {
    ReportResponse findById(UUID id);

    List<ReportResponse> findByAccountId(UUID accountId);

    ReportResponse createReport(CreateReportDTO report);
}
