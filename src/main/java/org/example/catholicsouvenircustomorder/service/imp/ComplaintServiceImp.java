package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.ApproveComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.ArtisanResponseRequest;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.CreateComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.RejectComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintResponse;
import org.example.catholicsouvenircustomorder.exception.InsufficientBalanceException;
import org.example.catholicsouvenircustomorder.exception.NotFoundException;
import org.example.catholicsouvenircustomorder.exception.UnauthorizedException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.CancellationService;
import org.example.catholicsouvenircustomorder.service.ComplaintService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.OfflineRecoveryService;
import org.example.catholicsouvenircustomorder.service.RefundCalculationService;
import org.example.catholicsouvenircustomorder.service.RefundService;
import org.example.catholicsouvenircustomorder.service.SystemConfigService;
import org.example.catholicsouvenircustomorder.util.VNPayUtil;
import org.example.catholicsouvenircustomorder.util.VNPayErrorMapper;
import org.example.catholicsouvenircustomorder.dto.response.VNPayRefundResponse;
import org.example.catholicsouvenircustomorder.dto.response.StageRefundCalculation;
import org.example.catholicsouvenircustomorder.exception.RefundProcessingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of ComplaintService
 * Handles complaint lifecycle: creation, artisan response, admin review, and queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintServiceImp implements ComplaintService {
    
    private final ComplaintRepository complaintRepository;
    private final OrderRepository orderRepository;
    private final CustomOrderRepository customOrderRepository;
    private final AccountRepository accountRepository;
    private final ArtisanRepository artisanRepository;
    private final WalletRepository walletRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final NotificationService notificationService;
    private final RefundService refundService;
    private final CancellationService cancellationService;
    private final RefundCalculationService refundCalculationService;
    private final OfflineRecoveryService offlineRecoveryService;
    private final StagePaymentRepository stagePaymentRepository;
    private final VNPayUtil vnPayUtil;
    private final SystemConfigService systemConfigService;
    
    /**
     * Create a new complaint for an order
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 10.1, 10.2, 10.3, 10.4, 11.2
     */
    @Override
    @Transactional
    public ComplaintResponse createComplaint(CreateComplaintRequest request, UUID customerId) {
        log.info("Creating complaint for customer: {}, orderId: {}, customOrderId: {}", 
                 customerId, request.getOrderId(), request.getCustomOrderId());
        
        // 1. Validate order exists and belongs to customer
        Order order = null;
        CustomOrder customOrder = null;
        Artisan artisan = null;
        
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NotFoundException("Đơn hàng không tồn tại"));
            
            if (!order.getCustomer().getAccountId().equals(customerId)) {
                throw new UnauthorizedException("Đơn hàng không thuộc về khách hàng này");
            }
            
            // Determine artisan from order details
            artisan = determineArtisanFromOrder(order);
        } else if (request.getCustomOrderId() != null) {
            customOrder = customOrderRepository.findById(request.getCustomOrderId())
                .orElseThrow(() -> new NotFoundException("Đơn hàng tùy chỉnh không tồn tại"));
            
            if (!customOrder.getRequest().getCustomer().getAccountId().equals(customerId)) {
                throw new UnauthorizedException("Đơn hàng tùy chỉnh không thuộc về khách hàng này");
            }
            
            artisan = customOrder.getArtisan();
        } else {
            throw new IllegalArgumentException("Phải cung cấp orderId hoặc customOrderId");
        }
        
        // 2. Validate order status is DELIVERED or COMPLETED
        if (order != null) {
            if (!"DELIVERED".equals(order.getStatus())) {
                throw new IllegalStateException("Chỉ có thể tạo khiếu nại cho đơn hàng đã giao");
            }
        } else {
            if (customOrder.getStatus() != CustomOrderStatus.COMPLETED) {
                throw new IllegalStateException("Chỉ có thể tạo khiếu nại cho đơn hàng đã hoàn thành");
            }
        }
        
        // 3. Validate within 7-day window
        LocalDateTime deliveredDate = order != null ? order.getUpdateAt() : customOrder.getUpdatedAt();
        if (deliveredDate == null || deliveredDate.plusDays(7).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Khiếu nại phải được tạo trong vòng 7 ngày kể từ ngày giao hàng");
        }
        
        // 4. Check if complaint already exists
        boolean exists = complaintRepository.existsByOrderOrCustomOrder(
            order != null ? order.getOrderId() : null,
            customOrder != null ? customOrder.getCustomOrderId() : null
        );
        if (exists) {
            throw new IllegalArgumentException("Đơn khiếu nại đã tồn tại cho đơn hàng này");
        }
        
        // 5. Create complaint
        Account customer = accountRepository.findById(customerId)
            .orElseThrow(() -> new NotFoundException("Khách hàng không tồn tại"));
        
        Complaint complaint = new Complaint();
        complaint.setOrder(order);
        complaint.setCustomOrder(customOrder);
        complaint.setCustomer(customer);
        complaint.setArtisan(artisan);
        complaint.setReason(request.getReason());
        complaint.setEvidenceImages(request.getEvidenceImages());
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setWithdrawalFrozen(true); // Freeze withdrawals when complaint is created
        complaint.setCreatedAt(LocalDateTime.now());
        
        complaint = complaintRepository.save(complaint);
        
        log.info("Complaint created successfully: {}", complaint.getComplaintId());
        
        // 6. Send notification to artisan
        notificationService.sendNotification(
            artisan.getAccount().getAccountId(),
            NotificationType.COMPLAINT_CREATED,
            "Khiếu nại mới",
            "Bạn có một khiếu nại mới từ khách hàng " + customer.getFullName(),
            complaint.getComplaintId()
        );
        
        return mapToResponse(complaint);
    }

    /**
     * Artisan responds to complaint and decides on return requirement
     * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8
     */
    @Override
    @Transactional
    public ComplaintResponse respondToComplaint(UUID complaintId, ArtisanResponseRequest request, UUID artisanId) {
        log.info("Artisan {} responding to complaint: {}", artisanId, complaintId);
        
        // 1. Get complaint and validate
        Complaint complaint = complaintRepository.findById(complaintId)
            .orElseThrow(() -> new NotFoundException("Khiếu nại không tồn tại"));
        
        if (!complaint.getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new UnauthorizedException("Khiếu nại không thuộc về nghệ nhân này");
        }
        
        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể phản hồi khiếu nại đang chờ xử lý");
        }
        
        // 2. Update complaint with response
        complaint.setArtisanResponse(request.getResponse());
        complaint.setArtisanResponseAt(LocalDateTime.now());
        
        complaint = complaintRepository.save(complaint);
        
        log.info("Artisan response saved for complaint: {}", complaintId);
        
        // 3. Send notification to admin (platform admin)
        Account platformAdmin = accountRepository.findByRole_Name("ADMIN").stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Admin không tồn tại"));
        
        notificationService.sendNotification(
            platformAdmin.getAccountId(),
            NotificationType.ARTISAN_RESPONDED,
            "Nghệ nhân đã phản hồi khiếu nại",
            "Nghệ nhân " + complaint.getArtisan().getArtisanName() + " đã phản hồi khiếu nại #" + complaintId,
            complaint.getComplaintId()
        );
        
        return mapToResponse(complaint);
    }
    
    /**
     * Admin approves complaint
     * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.7, 4.1, 4.2, 6.1, 6.2, 6.3
     */
    @Override
    @Transactional
    public ComplaintResponse approveComplaint(UUID complaintId, ApproveComplaintRequest request, UUID adminId) {
        log.info("Admin {} approving complaint: {}", adminId, complaintId);
        
        // 1. Get complaint and validate
        Complaint complaint = complaintRepository.findById(complaintId)
            .orElseThrow(() -> new NotFoundException("Khiếu nại không tồn tại"));
        
        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể phê duyệt khiếu nại đang chờ xử lý");
        }
        
        // 2. Validate refund amount
        BigDecimal maxRefund = calculateMaxRefundAmount(complaint);
        if (request.getRefundAmount().compareTo(maxRefund) > 0) {
            throw new IllegalArgumentException("Số tiền hoàn vượt quá mức tối đa cho phép: " + maxRefund);
        }
        
        // 3. Update complaint
        Account admin = accountRepository.findById(adminId)
            .orElseThrow(() -> new NotFoundException("Admin không tồn tại"));
        
        complaint.setRefundAmount(request.getRefundAmount());
        complaint.setAdminNote(request.getAdminNote());
        complaint.setReviewedBy(admin);
        complaint.setReviewedAt(LocalDateTime.now());
        
        // 4. Process refund immediately (no return required)
        complaint.setStatus(ComplaintStatus.PROCESSING_REFUND);
        complaint = complaintRepository.save(complaint);
        
        try {
            RefundTransaction refundTransaction;
            
            // Check if this is a custom order - use custom refund calculation
            if (complaint.getCustomOrder() != null) {
                log.info("Processing custom order complaint refund with platform commission");
                
                // Calculate and process refund with platform commission
                refundTransaction = processCustomOrderComplaintRefund(
                    complaint,
                    request.getRefundAmount()
                );
                
                    // Process VNPay refund for custom order
                    processVNPayRefundForComplaint(refundTransaction, complaint.getCustomOrder());
                } else {
                    // Regular order - use existing RefundService (handles VNPay internally)
                    log.info("Processing regular order complaint refund using RefundService");
                    refundTransaction = refundService.processRefund(
                        complaint, 
                        request.getRefundAmount()
                    );
                }
                
                // Update complaint status to APPROVED and unfreeze withdrawals
                complaint.setStatus(ComplaintStatus.APPROVED);
                complaint.setWithdrawalFrozen(false); // Unfreeze withdrawals after refund
                complaint = complaintRepository.save(complaint);
                
                log.info("Complaint approved and VNPay refund processed: {}", complaintId);
                
                // Send success notifications - Updated for VNPay refund
                notificationService.sendNotification(
                    complaint.getCustomer().getAccountId(),
                    NotificationType.REFUND_COMPLETED,
                    "Hoàn tiền đang xử lý",
                    "Số tiền " + request.getRefundAmount() + " VND sẽ được hoàn về tài khoản ngân hàng của bạn trong 3-7 ngày làm việc qua VNPay.",
                    complaint.getComplaintId()
                );
                
                notificationService.sendNotification(
                    complaint.getArtisan().getAccount().getAccountId(),
                    NotificationType.COMPLAINT_APPROVED,
                    "Khiếu nại được phê duyệt",
                    "Khiếu nại đã được phê duyệt và số tiền " + request.getRefundAmount() + " VND sẽ được hoàn cho khách hàng qua VNPay.",
                    complaint.getComplaintId()
                );
            } catch (InsufficientBalanceException e) {
                // Refund failed due to insufficient artisan balance
                complaint.setStatus(ComplaintStatus.PENDING);
                complaint = complaintRepository.save(complaint);
                
                log.error("Refund failed for complaint {} due to insufficient artisan balance: {}", complaintId, e.getMessage());
                
                // Send notification to admin
                notificationService.sendNotification(
                    adminId,
                    NotificationType.REFUND_FAILED,
                    "Hoàn tiền thất bại - Số dư không đủ",
                    "Hoàn tiền cho khiếu nại #" + complaintId + " thất bại do số dư ví nghệ nhân không đủ: " + e.getMessage(),
                    complaint.getComplaintId()
                );
                
                throw e;
            } catch (RuntimeException e) {
                // Handle VNPay refund exceptions from RefundService
                complaint.setStatus(ComplaintStatus.PENDING);
                complaint = complaintRepository.save(complaint);
                
                log.error("VNPay refund failed for complaint {}: {}", complaintId, e.getMessage(), e);
                
                // Determine if this is a retryable error
                boolean isRetryable = e.getMessage() != null && 
                    (e.getMessage().contains("timeout") || 
                     e.getMessage().contains("network") || 
                     e.getMessage().contains("có thể thử lại"));
                
                String notificationTitle = isRetryable ? 
                    "Hoàn tiền VNPay thất bại - Có thể thử lại" : 
                    "Hoàn tiền VNPay thất bại - Cần xử lý thủ công";
                
                String notificationMessage = String.format(
                    "Hoàn tiền VNPay cho khiếu nại #%s thất bại. " +
                    "Khách hàng: %s, Số tiền: %,d VNĐ. " +
                    "Lỗi: %s. %s",
                    complaintId,
                    complaint.getCustomer().getFullName(),
                    request.getRefundAmount().longValue(),
                    e.getMessage(),
                    isRetryable ? "Có thể thử lại sau." : "Cần xử lý thủ công."
                );
                
                // Send notification to admin
                notificationService.sendNotification(
                    adminId,
                    NotificationType.REFUND_FAILED,
                    notificationTitle,
                    notificationMessage,
                    complaint.getComplaintId()
                );
                
                // Send notification to customer about the delay
                notificationService.sendNotification(
                    complaint.getCustomer().getAccountId(),
                    NotificationType.REFUND_FAILED,
                    "Hoàn tiền tạm thời gặp sự cố",
                    "Hoàn tiền cho khiếu nại của bạn đang gặp sự cố kỹ thuật. " +
                    "Chúng tôi sẽ xử lý và thông báo kết quả sớm nhất. " +
                    "Vui lòng liên hệ hỗ trợ nếu cần thêm thông tin.",
                    complaint.getComplaintId()
                );
                
                throw e;
            } catch (Exception e) {
                // Handle any other unexpected exceptions
                complaint.setStatus(ComplaintStatus.PENDING);
                complaint = complaintRepository.save(complaint);
                
                log.error("Unexpected error during refund processing for complaint {}: {}", complaintId, e.getMessage(), e);
                
                // Send notification to admin
                notificationService.sendNotification(
                    adminId,
                    NotificationType.REFUND_FAILED,
                    "Hoàn tiền thất bại - Lỗi hệ thống",
                    "Hoàn tiền cho khiếu nại #" + complaintId + " thất bại do lỗi hệ thống: " + e.getMessage() + ". Cần kiểm tra và xử lý thủ công.",
                    complaint.getComplaintId()
                );
                
                throw new RuntimeException("Lỗi hệ thống khi xử lý hoàn tiền: " + e.getMessage(), e);
            }
        
        return mapToResponse(complaint);
    }
    
    /**
     * Admin rejects complaint
     * Requirements: 3.6, 3.7, 6.3
     */
    @Override
    @Transactional
    public ComplaintResponse rejectComplaint(UUID complaintId, RejectComplaintRequest request, UUID adminId) {
        log.info("Admin {} rejecting complaint: {}", adminId, complaintId);
        
        // 1. Get complaint and validate
        Complaint complaint = complaintRepository.findById(complaintId)
            .orElseThrow(() -> new NotFoundException("Khiếu nại không tồn tại"));
        
        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể từ chối khiếu nại đang chờ xử lý");
        }
        
        // 2. Update complaint
        Account admin = accountRepository.findById(adminId)
            .orElseThrow(() -> new NotFoundException("Admin không tồn tại"));
        
        complaint.setRejectionReason(request.getRejectionReason());
        complaint.setReviewedBy(admin);
        complaint.setReviewedAt(LocalDateTime.now());
        complaint.setStatus(ComplaintStatus.REJECTED);
        complaint.setWithdrawalFrozen(false); // Unfreeze withdrawals when rejected
        
        complaint = complaintRepository.save(complaint);
        
        log.info("Complaint rejected: {}", complaintId);
        
        // 3. Send notification to customer
        notificationService.sendNotification(
            complaint.getCustomer().getAccountId(),
            NotificationType.COMPLAINT_REJECTED,
            "Khiếu nại bị từ chối",
            "Khiếu nại của bạn đã bị từ chối. Lý do: " + request.getRejectionReason(),
            complaint.getComplaintId()
        );
        
        return mapToResponse(complaint);
    }

    /**
     * Get complaint details with authorization check
     * Requirements: 7.1, 7.2
     */
    @Override
    @Transactional(readOnly = true)
    public ComplaintDetailResponse getComplaintDetails(UUID complaintId, UUID userId) {
        log.info("Getting complaint details: {} for user: {}", complaintId, userId);
        
        Complaint complaint = complaintRepository.findById(complaintId)
            .orElseThrow(() -> new NotFoundException("Khiếu nại không tồn tại"));
        
        // Authorization check: Only customer, artisan, or admin can view
        boolean isCustomer = complaint.getCustomer().getAccountId().equals(userId);
        boolean isArtisan = complaint.getArtisan().getAccount().getAccountId().equals(userId);
        
        Account user = accountRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        boolean isAdmin = user.getRole().getName().equals("ADMIN");
        
        if (!isCustomer && !isArtisan && !isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền xem khiếu nại này");
        }
        
        return mapToDetailResponse(complaint);
    }
    
    /**
     * List complaints for customer with pagination
     * Requirements: 7.1, 7.2
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getCustomerComplaints(UUID customerId, Pageable pageable) {
        log.info("Getting complaints for customer: {}", customerId);
        
        Account customer = accountRepository.findById(customerId)
            .orElseThrow(() -> new NotFoundException("Khách hàng không tồn tại"));
        
        Page<Complaint> complaints = complaintRepository.findByCustomer(customer, pageable);
        return complaints.map(this::mapToResponse);
    }
    
    /**
     * List complaints for artisan with pagination
     * Requirements: 8.1, 8.2
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getArtisanComplaints(UUID artisanId, Pageable pageable) {
        log.info("Getting complaints for artisan: {}", artisanId);
        
        Artisan artisan = artisanRepository.findById(artisanId)
            .orElseThrow(() -> new NotFoundException("Nghệ nhân không tồn tại"));
        
        Page<Complaint> complaints = complaintRepository.findByArtisan(artisan, pageable);
        return complaints.map(this::mapToResponse);
    }
    
    /**
     * List all complaints for admin with filter by status
     * Requirements: 3.1
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getAllComplaints(ComplaintStatus status, Pageable pageable) {
        log.info("Getting all complaints with status: {}", status);
        
        Page<Complaint> complaints;
        if (status != null) {
            complaints = complaintRepository.findByStatus(status, pageable);
        } else {
            complaints = complaintRepository.findAll(pageable);
        }
        
        return complaints.map(this::mapToResponse);
    }
    
    /**
     * Validate if order is eligible for complaint
     * Requirements: 1.4, 1.5
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isEligibleForComplaint(UUID orderId, UUID customOrderId) {
        if (orderId != null) {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) return false;
            
            // Check status is DELIVERED
            if (!"DELIVERED".equals(order.getStatus())) return false;
            
            // Check within 7 days
            LocalDateTime deliveredDate = order.getUpdateAt();
            if (deliveredDate == null || deliveredDate.plusDays(7).isBefore(LocalDateTime.now())) {
                return false;
            }
            
            // Check no existing complaint
            return !complaintRepository.existsByOrderOrCustomOrder(orderId, null);
        } else if (customOrderId != null) {
            CustomOrder customOrder = customOrderRepository.findById(customOrderId).orElse(null);
            if (customOrder == null) return false;
            
            // Check status is COMPLETED
            if (customOrder.getStatus() != CustomOrderStatus.COMPLETED) return false;
            
            // Check within 7 days
            LocalDateTime deliveredDate = customOrder.getUpdatedAt();
            if (deliveredDate == null || deliveredDate.plusDays(7).isBefore(LocalDateTime.now())) {
                return false;
            }
            
            // Check no existing complaint
            return !complaintRepository.existsByOrderOrCustomOrder(null, customOrderId);
        }
        
        return false;
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Process custom order complaint refund using cancellation logic
     * Uses same refund calculation and insurance fund logic as cancellation
     * Requirements: 4.3, 4.4, 4.5
     */
    private RefundTransaction processCustomOrderComplaintRefund(
        Complaint complaint,
        BigDecimal adminApprovedAmount
    ) {
        CustomOrder customOrder = complaint.getCustomOrder();
        
        log.info("Processing custom order complaint refund for order: {}", customOrder.getCustomOrderId());
        
        // Calculate refund using same logic as cancellation (100% for all paid stages)
        List<StageRefundCalculation> stageRefunds = refundCalculationService
            .calculateStageRefunds(customOrder, CancellationInitiator.CUSTOMER);
        
        BigDecimal calculatedRefundAmount = stageRefunds.stream()
            .map(StageRefundCalculation::getNetRefund)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal platformCommission = stageRefunds.stream()
            .map(StageRefundCalculation::getPlatformCommission)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Calculated refund amount: {} VND, Platform commission: {} VND, Admin approved: {} VND", 
            calculatedRefundAmount, platformCommission, adminApprovedAmount);
        
        // Use the admin approved amount (which should not exceed calculated amount)
        BigDecimal finalRefundAmount = adminApprovedAmount;
        
        // Get artisan wallet and check available balance
        Wallet artisanWallet = walletRepository.findByAccount(customOrder.getArtisan().getAccount())
            .orElseThrow(() -> new NotFoundException("Artisan wallet not found"));
        
        BigDecimal availableBalance = artisanWallet.getAvailableBalance();
        
        log.info("Artisan available balance: {} VND, Required refund: {} VND", 
            availableBalance, finalRefundAmount);
        
        // Check if artisan has sufficient balance
        if (availableBalance.compareTo(finalRefundAmount) < 0) {
            log.error("Insufficient balance for complaint refund: available={}, required={}", 
                availableBalance, finalRefundAmount);
            
            // Create offline recovery task
            offlineRecoveryService.createRecoveryTask(
                customOrder,
                finalRefundAmount,
                "Complaint refund - Artisan insufficient balance"
            );
            
            throw new InsufficientBalanceException(
                String.format("Artisan không đủ số dư để hoàn tiền. Cần: %s VND, Có: %s VND. " +
                    "Đã tạo task offline recovery.",
                    finalRefundAmount, availableBalance)
            );
        }
        
        // Deduct from artisan wallet
        artisanWallet.setBalance(artisanWallet.getBalance().subtract(finalRefundAmount));
        walletRepository.save(artisanWallet);
        
        log.info("Deducted {} VND from artisan wallet for complaint refund", finalRefundAmount);
        
        // Create RefundTransaction record
        RefundTransaction refundTransaction = new RefundTransaction();
        refundTransaction.setCustomOrder(customOrder);
        refundTransaction.setRefundSource(RefundSource.COMPLAINT);
        refundTransaction.setComplaint(complaint);
        refundTransaction.setAmount(finalRefundAmount.add(platformCommission)); // Gross amount
        refundTransaction.setPlatformCommissionAmount(platformCommission);
        refundTransaction.setNetRefundAmount(finalRefundAmount);
        refundTransaction.setFromWallet(artisanWallet);
        refundTransaction.setStatus(RefundStatus.PENDING);
        
        // Store calculation details
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String calculationJson = objectMapper.writeValueAsString(stageRefunds);
            refundTransaction.setCalculationDetails(calculationJson);
        } catch (Exception e) {
            log.error("Failed to serialize refund calculation details", e);
        }
        
        refundTransaction = refundTransactionRepository.save(refundTransaction);
        
        log.info("Custom order complaint refund transaction created: {}", 
            refundTransaction.getRefundTransactionId());
        
        return refundTransaction;
    }
    
    /**
     * Process VNPay refund for custom order complaint
     * Similar to CancellationService.processVNPayRefund() but for complaints
     * Requirements: 4.3, 4.4, 4.5
     */
    private void processVNPayRefundForComplaint(RefundTransaction refundTransaction, CustomOrder order) {
        log.info("Processing VNPay refund for complaint refund transaction {}", 
            refundTransaction.getRefundTransactionId());
        
        // Get all paid stages
        List<CustomOrderStage> paidStages = order.getStages().stream()
            .filter(CustomOrderStage::getIsPaid)
            .toList();
        
        if (paidStages.isEmpty()) {
            log.warn("No paid stages found for order {}", order.getCustomOrderId());
            refundTransaction.setStatus(RefundStatus.FAILED);
            refundTransaction.setFailureReason("Không có stage nào đã thanh toán");
            refundTransactionRepository.save(refundTransaction);
            throw new RefundProcessingException("Không có stage nào đã thanh toán", 
                RefundProcessingException.PAYMENT_NOT_FOUND);
        }
        
        // Calculate total refund amount and proportional amounts per stage
        BigDecimal totalRefundAmount = refundTransaction.getNetRefundAmount();
        BigDecimal totalPaidAmount = paidStages.stream()
            .map(CustomOrderStage::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Total paid amount: {}, Total refund amount: {}", totalPaidAmount, totalRefundAmount);
        
        List<String> vnpayRefundIds = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        // Process refund for each paid stage
        for (CustomOrderStage stage : paidStages) {
            try {
                // Find the successful payment for this stage
                StagePayment stagePayment = stagePaymentRepository
                    .findFirstByStage_StageIdAndStatusOrderByPaidAtDesc(
                        stage.getStageId(), 
                        PaymentStatus.SUCCESS
                    )
                    .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy thanh toán thành công cho stage " + stage.getName()
                    ));
                
                // Calculate proportional refund for this stage
                BigDecimal stageRefundAmount = calculateProportionalRefund(
                    stage.getAmount(),
                    totalRefundAmount,
                    totalPaidAmount
                );
                
                log.info("Processing refund for stage {}: {} VND (from original {})", 
                    stage.getName(), stageRefundAmount, stage.getAmount());
                
                // Validate payment has paidAt date
                if (stagePayment.getPaidAt() == null) {
                    String errorMsg = String.format("Stage %s không có ngày thanh toán", stage.getName());
                    log.error(errorMsg);
                    failureReasons.add(errorMsg);
                    failCount++;
                    continue;
                }
                
                // Convert payment date to VNPay format
                String originalTransactionDate = vnPayUtil.formatVNPayDate(stagePayment.getPaidAt());
                
                // Call VNPay refund API
                VNPayRefundResponse vnpayResponse = vnPayUtil.createRefundRequest(
                    stagePayment.getReferenceId(),
                    stagePayment.getTransactionId(),
                    originalTransactionDate,
                    stageRefundAmount,
                    "Hoàn tiền khiếu nại #" + refundTransaction.getComplaint().getComplaintId()
                );
                
                // Check VNPay response
                if (!VNPayErrorMapper.isSuccess(vnpayResponse.getResponseCode())) {
                    String errorMsg = String.format("VNPay từ chối hoàn tiền stage %s. Mã lỗi: %s - %s",
                        stage.getName(), vnpayResponse.getResponseCode(), vnpayResponse.getMessage());
                    log.error(errorMsg);
                    failureReasons.add(errorMsg);
                    failCount++;
                    continue;
                }
                
                // Store VNPay refund ID
                vnpayRefundIds.add(vnpayResponse.getVnpayRefundId());
                successCount++;
                
                // Store the first refund ID as primary
                if (refundTransaction.getVnpayRefundId() == null) {
                    refundTransaction.setVnpayRefundId(vnpayResponse.getVnpayRefundId());
                    refundTransaction.setVnpayTransactionNo(vnpayResponse.getVnpayTransactionNo());
                    refundTransaction.setOriginalPaymentId(stagePayment.getPaymentId());
                }
                
                log.info("Stage refund successful. VNPay Refund ID: {}", vnpayResponse.getVnpayRefundId());
                
            } catch (Exception e) {
                String errorMsg = String.format("Lỗi khi hoàn tiền stage %s: %s", 
                    stage.getName(), e.getMessage());
                log.error(errorMsg, e);
                failureReasons.add(errorMsg);
                failCount++;
            }
        }
        
        // Update refund transaction status based on results
        if (failCount == 0) {
            refundTransaction.setStatus(RefundStatus.PROCESSING);
            log.info("All stage refunds initiated successfully");
        } else if (successCount > 0) {
            refundTransaction.setStatus(RefundStatus.PARTIALLY_REFUNDED);
            refundTransaction.setFailureReason(String.join("; ", failureReasons));
            log.warn("Partial refund: {} succeeded, {} failed", successCount, failCount);
        } else {
            refundTransaction.setStatus(RefundStatus.FAILED);
            refundTransaction.setFailureReason(String.join("; ", failureReasons));
            log.error("All stage refunds failed");
            throw new RefundProcessingException("Tất cả stage refunds thất bại: " + String.join("; ", failureReasons),
                RefundProcessingException.PARTIAL_REFUND_FAILURE);
        }
        
        refundTransactionRepository.save(refundTransaction);
        
        log.info("VNPay refund processing completed. Success: {}, Failed: {}", successCount, failCount);
    }
    
    /**
     * Calculate proportional refund amount for a stage
     */
    private BigDecimal calculateProportionalRefund(
        BigDecimal stageAmount,
        BigDecimal totalRefundAmount,
        BigDecimal totalPaidAmount
    ) {
        if (totalPaidAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate proportion: (stageAmount / totalPaidAmount) * totalRefundAmount
        BigDecimal proportion = stageAmount.divide(totalPaidAmount, 10, java.math.RoundingMode.HALF_UP);
        BigDecimal refundAmount = proportion.multiply(totalRefundAmount);
        
        // Round to 2 decimal places
        return refundAmount.setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate max refund amount based on total order amount (including shipping) minus platform commission
     * Customer paid total amount, so they should get refunded the full amount (minus platform fee)
     * Artisan bears both product cost and shipping cost when complaint is approved
     * Requirements: 10.5, 10.6
     */
    private BigDecimal calculateMaxRefundAmount(Complaint complaint) {
        BigDecimal totalAmount;
        
        if (complaint.getOrder() != null) {
            // For regular order: Get total amount (including shipping)
            totalAmount = complaint.getOrder().getTotal();
        } else {
            // For custom order: Get total price (no shipping fee included)
            totalAmount = complaint.getCustomOrder().getTotalPrice();
        }
        
        // Get platform commission rate from system config
        BigDecimal commissionRate = systemConfigService.getCommissionRate();
        
        // Calculate max refund = totalAmount * (1 - commissionRate/100)
        // This is what customer should get back (total paid minus platform fee)
        BigDecimal commissionMultiplier = BigDecimal.ONE.subtract(
            commissionRate.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP)
        );
        
        return totalAmount.multiply(commissionMultiplier)
            .setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Determine artisan from order (handles both Order and CustomOrder)
     * Requirements: 10.3, 10.4
     */
    private Artisan determineArtisanFromOrder(Order order) {
        // Try to get artisan from product-based order details
        Artisan artisan = artisanRepository.findByOrderIdFromProduct(order.getOrderId()).orElse(null);
        
        // If not found, try template-based order details
        if (artisan == null) {
            artisan = artisanRepository.findByOrderIdFromTemplate(order.getOrderId()).orElse(null);
        }
        
        if (artisan == null) {
            throw new NotFoundException("Không tìm thấy nghệ nhân cho đơn hàng này");
        }
        
        return artisan;
    }
    
    /**
     * Map Complaint entity to ComplaintResponse DTO
     */
    private ComplaintResponse mapToResponse(Complaint complaint) {
        return ComplaintResponse.builder()
            .complaintId(complaint.getComplaintId())
            .orderId(complaint.getOrder() != null ? complaint.getOrder().getOrderId() : null)
            .customOrderId(complaint.getCustomOrder() != null ? complaint.getCustomOrder().getCustomOrderId() : null)
            .customerId(complaint.getCustomer().getAccountId())
            .customerName(complaint.getCustomer().getFullName())
            .artisanId(complaint.getArtisan().getArtisanUuid())
            .artisanName(complaint.getArtisan().getArtisanName())
            .reason(complaint.getReason())
            .status(complaint.getStatus())
            .refundAmount(complaint.getRefundAmount())
            .createdAt(complaint.getCreatedAt())
            .updatedAt(complaint.getUpdatedAt())
            .build();
    }
    
    /**
     * Map Complaint entity to ComplaintDetailResponse DTO
     */
    private ComplaintDetailResponse mapToDetailResponse(Complaint complaint) {
        return ComplaintDetailResponse.builder()
            .complaintId(complaint.getComplaintId())
            .orderId(complaint.getOrder() != null ? complaint.getOrder().getOrderId() : null)
            .customOrderId(complaint.getCustomOrder() != null ? complaint.getCustomOrder().getCustomOrderId() : null)
            .status(complaint.getStatus())
            .customerId(complaint.getCustomer().getAccountId())
            .customerName(complaint.getCustomer().getFullName())
            .customerEmail(complaint.getCustomer().getEmail())
            .artisanId(complaint.getArtisan().getArtisanUuid())
            .artisanName(complaint.getArtisan().getArtisanName())
            .reason(complaint.getReason())
            .evidenceImages(complaint.getEvidenceImages())
            .artisanResponse(complaint.getArtisanResponse())
            .artisanResponseAt(complaint.getArtisanResponseAt())
            .refundAmount(complaint.getRefundAmount())
            .adminNote(complaint.getAdminNote())
            .rejectionReason(complaint.getRejectionReason())
            .reviewedBy(complaint.getReviewedBy() != null ? complaint.getReviewedBy().getAccountId() : null)
            .reviewedByName(complaint.getReviewedBy() != null ? complaint.getReviewedBy().getFullName() : null)
            .reviewedAt(complaint.getReviewedAt())
            .createdAt(complaint.getCreatedAt())
            .updatedAt(complaint.getUpdatedAt())
            .refundTransactionId(complaint.getRefundTransaction() != null ? complaint.getRefundTransaction().getRefundTransactionId() : null)
            .refundStatus(complaint.getRefundTransaction() != null ? complaint.getRefundTransaction().getStatus().name() : null)
            .build();
    }
}
