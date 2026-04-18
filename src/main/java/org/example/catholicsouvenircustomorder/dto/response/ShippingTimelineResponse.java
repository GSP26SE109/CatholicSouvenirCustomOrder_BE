package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.ShippingStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingTimelineResponse {
    
    private ShippingStatus currentStatus;
    private String currentStatusLabel;
    private String currentStatusDescription;
    private List<TimelineStep> timeline;
    private String statusHistory;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineStep {
        private ShippingStatus status;
        private String label;
        private String description;
        private boolean completed;
        private boolean current;
        private LocalDateTime completedAt;
    }
}
