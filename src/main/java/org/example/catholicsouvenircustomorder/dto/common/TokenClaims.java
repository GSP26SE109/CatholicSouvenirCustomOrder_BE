package org.example.catholicsouvenircustomorder.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenClaims {
    private UUID accountId;
    private String role;
}
