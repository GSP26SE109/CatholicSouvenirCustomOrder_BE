package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;
    
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;
    
    @Size(max = 10, message = "Gender must not exceed 10 characters")
    private String gender;
    
    @Size(max = 50, message = "Date of birth must not exceed 50 characters")
    private String dateOfBirth;
    
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;
    
    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;
    
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;
    
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;
    
    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;
    
    @Size(max = 100, message = "Ward must not exceed 100 characters")
    private String ward;
    
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;
    
    @Size(max = 100, message = "Saint name must not exceed 100 characters")
    private String saintName;
    
    @Size(max = 10, message = "Language must not exceed 10 characters")
    private String language;
    
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;
}
