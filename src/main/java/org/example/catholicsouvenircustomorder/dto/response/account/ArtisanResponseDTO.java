package org.example.catholicsouvenircustomorder.dto.response.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtisanResponseDTO {
    private String artisanName;
    private String bio;
    private int experience_year;
    private String portfolioUrl;
    private String specialization;

}
