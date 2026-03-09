package org.example.catholicsouvenircustomorder.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class AcceptCustomRequestRequest {
    private UUID requestId;
}
