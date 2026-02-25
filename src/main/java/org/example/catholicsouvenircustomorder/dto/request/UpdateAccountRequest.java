package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateAccountRequest {
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;
    private String gender;
    private String dateOfBirth;
    private String avtUrl;
    private int roleId;
    private UUID saintId;
    private Boolean isVerified;
}
