package org.example.catholicsouvenircustomorder.service.imp;

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
import org.example.catholicsouvenircustomorder.service.ComplaintService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.RefundService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final NotificationService notificationService;
    private final RefundService refundService;
    
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
        complaint.setRequireReturn(request.getRequireReturn());
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
        
        // 4. Process based on requireReturn flag
        if (complaint.getRequireReturn()) {
            // Require return: Update status to WAITING_RETURN
            complaint.setStatus(ComplaintStatus.WAITING_RETURN);
            complaint = complaintRepository.save(complaint);
            
            log.info("Complaint approved with return required: {}", complaintId);
            
            // Send notification to customer with return instructions
            notificationService.sendNotification(
                complaint.getCustomer().getAccountId(),
                NotificationType.COMPLAINT_APPROVED,
                "Khiếu nại được phê duyệt",
                "Khiếu nại của bạn đã được phê duyệt. Vui lòng trả hàng về cho nghệ nhân.",
                complaint.getComplaintId()
            );
        } else {
            // No return required: Process refund immediately
            complaint.setStatus(ComplaintStatus.PROCESSING_REFUND);
            complaint = complaintRepository.save(complaint);
            
            try {
                // Process refund via VNPay
                RefundTransaction refundTransaction = refundService.processRefund(
                    complaint, 
                    request.getRefundAmount()
                );
                
                // Update complaint status to APPROVED
                complaint.setStatus(ComplaintStatus.APPROVED);
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
     * Calculate max refund amount (90% of order total)
     * Requirements: 10.5, 10.6
     */
    private BigDecimal calculateMaxRefundAmount(Complaint complaint) {
        BigDecimal totalPaid;
        
        if (complaint.getOrder() != null) {
            // For regular order: Get total from order
            totalPaid = complaint.getOrder().getTotal();
        } else {
            // For custom order: Get total from custom order
            totalPaid = complaint.getCustomOrder().getTotalPrice();
        }
        
        // Max refund = 90% of total (excluding 10% platform fee)
        return totalPaid.multiply(new BigDecimal("0.9"));
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
            .requireReturn(complaint.getRequireReturn())
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
