package org.example.catholicsouvenircustomorder.dto.response.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtisanResponseDTO {
    private UUID artisanId;
    private String artisanName;
    private String phoneNumber;
    private String profileImageUrl;
    private String bio;
    private String specialization;
    private Integer experienceYears;
    private String portfolioUrl;
    
    // Rating info
}
