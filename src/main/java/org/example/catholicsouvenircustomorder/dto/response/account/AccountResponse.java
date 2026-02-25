package org.example.catholicsouvenircustomorder.dto.response.account;

import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Data
public class AccountResponse {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String passWord;
    private LocalDateTime created_date;
    private LocalDateTime updated_date;
}
