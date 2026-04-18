package org.example.catholicsouvenircustomorder.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateArtisanProfileRequest {
    private String artisanName;
    private String bio;
}
