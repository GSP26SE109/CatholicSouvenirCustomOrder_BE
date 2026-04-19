package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Interface projection for complaint statistics
 * Used by Spring Data JPA to map query results efficiently
 */
public interface ComplaintStatistics {
    Long getTotalComplaints();
    Long getPendingComplaints();
    Long getApprovedComplaints();
    Long getRejectedComplaints();
    BigDecimal getTotalRefundAmount();
}
