package org.example.catholicsouvenircustomorder.dto.response.Complaint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceResponse {
    private List<String> imageUrl;
    private String message;

    private UUID uploadedBy;
}
