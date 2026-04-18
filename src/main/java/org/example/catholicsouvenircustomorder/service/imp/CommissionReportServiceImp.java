package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.response.CommissionReportItem;
import org.example.catholicsouvenircustomorder.dto.response.CommissionReportResponse;
import org.example.catholicsouvenircustomorder.model.WalletTransaction;
import org.example.catholicsouvenircustomorder.repository.WalletTransactionRepository;
import org.example.catholicsouvenircustomorder.service.CommissionReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of CommissionReportService
 * Generates commission reports with various grouping options
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommissionReportServiceImp implements CommissionReportService {
    
    private final WalletTransactionRepository walletTransactionRepository;
    
    @Override
    public CommissionReportResponse generateReport(LocalDate startDate, LocalDate endDate, GroupBy groupBy) {
        log.info("Generating commission report from {} to {} grouped by {}", startDate, endDate, groupBy);
        
        // Convert LocalDate to LocalDateTime for query
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        // Query wallet transactions with commission_fee > 0 in the date range using optimized query
        List<WalletTransaction> transactions = walletTransactionRepository
            .findTransactionsWithCommissionInDateRange(startDateTime, endDateTime);
        
        log.debug("Found {} transactions with commission in date range", transactions.size());
        
        // Calculate totals
        BigDecimal totalCommission = transactions.stream()
            .map(WalletTransaction::getCommissionFee)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalTransactions = transactions.size();
        
        BigDecimal averageCommission = totalTransactions > 0 
            ? totalCommission.divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        // Group data by date/week/month
        List<CommissionReportItem> items = groupTransactions(transactions, groupBy);
        
        log.info("Report generated: total_commission={}, total_transactions={}, average={}", 
            totalCommission, totalTransactions, averageCommission);
        
        return CommissionReportResponse.builder()
            .totalCommission(totalCommission)
            .totalTransactions(totalTransactions)
            .averageCommissionPerTransaction(averageCommission)
            .items(items)
            .build();
    }
    
    /**
     * Group transactions by date, week, or month
     */
    private List<CommissionReportItem> groupTransactions(List<WalletTransaction> transactions, GroupBy groupBy) {
        Map<LocalDate, List<WalletTransaction>> grouped = new LinkedHashMap<>();
        
        for (WalletTransaction tx : transactions) {
            LocalDate groupKey = getGroupKey(tx.getCreatedAt().toLocalDate(), groupBy);
            grouped.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(tx);
        }
        
        // Convert to CommissionReportItem list
        return grouped.entrySet().stream()
            .map(entry -> {
                LocalDate date = entry.getKey();
                List<WalletTransaction> txList = entry.getValue();
                
                BigDecimal totalCommission = txList.stream()
                    .map(WalletTransaction::getCommissionFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                long count = txList.size();
                
                return CommissionReportItem.builder()
                    .date(date)
                    .totalCommission(totalCommission)
                    .transactionCount(count)
                    .build();
            })
            .sorted(Comparator.comparing(CommissionReportItem::getDate))
            .collect(Collectors.toList());
    }
    
    /**
     * Get the group key based on groupBy option
     */
    private LocalDate getGroupKey(LocalDate date, GroupBy groupBy) {
        switch (groupBy) {
            case DAY:
                return date;
            case WEEK:
                // Get the first day of the week (Monday)
                return date.with(WeekFields.ISO.dayOfWeek(), 1);
            case MONTH:
                // Get the first day of the month
                return date.with(TemporalAdjusters.firstDayOfMonth());
            default:
                return date;
        }
    }
}
