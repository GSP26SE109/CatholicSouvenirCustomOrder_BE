package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResponse {
    
    private UUID feedbackId;
    private UUID orderId;
    private UUID customOrderId;
    private UUID customerId;
    private String customerName;
    private String customerAvatar;
    private UUID artisanId;
    private String artisanName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
