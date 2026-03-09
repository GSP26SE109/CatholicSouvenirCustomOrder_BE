package org.example.catholicsouvenircustomorder.dto.response;

import lombok.Data;
import org.example.catholicsouvenircustomorder.model.CustomRequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CustomRequestResponse {
    private UUID requestId;
    private UUID customerId;
    private String customerName;
    private String title;
    private String description;
    private String referenceImageUrl;
    private String aiGeneratedImageUrl;
    private CustomRequestStatus status;
    private List<ArtisanBasicInfo> selectedArtisans;
    private ArtisanBasicInfo confirmedArtisan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    public static class ArtisanBasicInfo {
        private UUID artisanId;
        private String artisanName;
        private String specialization;
    }
}
