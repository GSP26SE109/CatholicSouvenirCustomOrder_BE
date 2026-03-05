package org.example.catholicsouvenircustomorder.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.Role;

import java.util.UUID;

@Data
@Builder
public class AuthenResponse {
    private UUID accountId;
    private String email;
    private String roleName;
    private String token;
}
