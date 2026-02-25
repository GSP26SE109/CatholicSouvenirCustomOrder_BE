package org.example.catholicsouvenircustomorder.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ArtisanApplicationResponse {
    private UUID applicationId;
    private UUID accountId;
    private String accountEmail;
    private String accountFullName;
    private String artisanName;
    private String bio;
    private int experienceYear;
    private String portfolioUrl;
    private String specialization;
    private ApplicationStatus status;
    private String rejectionReason;
    private LocalDateTime submittedDate;
    private LocalDateTime reviewedDate;
    private String reviewedByName;
    private String message;
}
