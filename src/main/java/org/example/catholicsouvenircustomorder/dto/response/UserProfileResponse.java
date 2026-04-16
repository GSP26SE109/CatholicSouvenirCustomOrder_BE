package org.example.catholicsouvenircustomorder.dto.response;

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
public class UserProfileResponse {
    
    private UUID profileId;
    private UUID accountId;
    
    // Basic Information
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private String dateOfBirth;
    private String avatarUrl;
    
    // Additional Information
    private String bio;
    private String address;
    private String city;
    private String district;
    private String ward;
    private String postalCode;
    
    // Saint (simplified)
    private String saintName;
    
    // Preferences
    private String language;
    private String timezone;
    
    // Metadata
    private String roleName;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
