package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.CommissionReportResponse;

import java.time.LocalDate;

/**
 * Service for generating commission reports
 * Provides analytics on platform commission collection
 */
public interface CommissionReportService {
    
    /**
     * Generate commission report for a date range
     * 
     * @param startDate Start date of the report period
     * @param endDate End date of the report period
     * @param groupBy Grouping option (DAY, WEEK, MONTH)
     * @return CommissionReportResponse with aggregated data
     */
    CommissionReportResponse generateReport(LocalDate startDate, LocalDate endDate, GroupBy groupBy);
    
    /**
     * Enum for report grouping options
     */
    enum GroupBy {
        DAY,
        WEEK,
        MONTH
    }
}
