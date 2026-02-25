package org.example.catholicsouvenircustomorder.dto.request.account;

import lombok.Data;

@Data
public class AccountRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String passWord;

}
