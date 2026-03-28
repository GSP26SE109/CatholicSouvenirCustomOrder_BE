package org.example.catholicsouvenircustomorder.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SendToArtisansRequest {
    private UUID requestId;
    private List<UUID> artisanIds;
}
