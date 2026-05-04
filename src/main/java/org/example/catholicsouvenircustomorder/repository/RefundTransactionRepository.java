package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Complaint;
import org.example.catholicsouvenircustomorder.model.RefundStatus;
import org.example.catholicsouvenircustomorder.model.RefundTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, UUID> {
    
    /**
     * Find refund transaction by complaint
     * Requirements: 9.1
     */
    Optional<RefundTransaction> findByComplaint(Complaint complaint);
    
    /**
     * Find all refund transactions by status with pagination
     * Requirements: 9.1
     */
    Page<RefundTransaction> findByStatus(RefundStatus status, Pageable pageable);
}
