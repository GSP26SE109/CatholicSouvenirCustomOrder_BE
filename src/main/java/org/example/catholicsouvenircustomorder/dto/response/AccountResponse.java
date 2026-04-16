package org.example.catholicsouvenircustomorder.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AccountResponse {
    private UUID accountId;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private String dateOfBirth;
    private String avtUrl;
    private boolean isVerified;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String roleName;
    private int roleId;
}
