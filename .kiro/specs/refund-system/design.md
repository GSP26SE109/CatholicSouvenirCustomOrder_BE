# Design Document - Hệ Thống Hoàn Tiền

## Overview

Hệ thống hoàn tiền được thiết kế để xử lý khiếu nại và hoàn tiền một cách tự động, công bằng và an toàn. Hệ thống tích hợp với các module hiện có (Wallet, Notification, Order, CustomOrder) và hỗ trợ cả 3 luồng đặt hàng.

**Các thành phần chính:**
- Complaint Management: Quản lý đơn khiếu nại
- Artisan Response: Phản hồi và quyết định trả hàng của Artisan
- Admin Review: Xem xét và phê duyệt/từ chối
- Refund Processing: Xử lý hoàn tiền tự động
- Return Shipment: Quản lý trả hàng (optional)

## Architecture

### High-Level Architecture

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│  Customer   │────────▶│  Complaint   │◀────────│   Artisan   │
│   (Mobile)  │         │  Controller  │         │   (Mobile)  │
└─────────────┘         └──────────────┘         └─────────────┘
                               │
                               ▼
                        ┌──────────────┐
                        │  Complaint   │
                        │   Service    │
                        └──────────────┘
                               │
                ┌──────────────┼──────────────┐
                ▼              ▼              ▼
         ┌───────────┐  ┌───────────┐  ┌───────────┐
         │  Wallet   │  │Notification│  │  Order    │
         │  Service  │  │  Service   │  │  Service  │
         └───────────┘  └───────────┘  └───────────┘
                │
                ▼
         ┌───────────────┐
         │ Refund        │
         │ Transaction   │
         └───────────────┘
```

### Component Interaction Flow

**1. Tạo Đơn Khiếu Nại:**
```
Customer → ComplaintController → ComplaintService
    → Validate Order (DELIVERED, within 7 days)
    → Create Complaint (status: PENDING)
    → NotificationService → Send to Artisan
```

**2. Artisan Phản Hồi:**
```
Artisan → ComplaintController → ComplaintService
    → Update Complaint (response, requireReturn)
    → NotificationService → Send to Admin
```

**3. Admin Phê Duyệt:**
```
Admin → ComplaintController → ComplaintService
    → Update Complaint (status: APPROVED/REJECTED)
    → IF APPROVED AND !requireReturn:
        → RefundService → Process Refund
    → IF APPROVED AND requireReturn:
        → Update status to WAITING_RETURN
    → NotificationService → Send to Customer & Artisan
```

**4. Xử Lý Hoàn Tiền:**
```
RefundService → WalletService
    → Check Artisan Wallet Balance
    → IF sufficient:
        → Deduct from Artisan Wallet
        → Add to Customer Wallet
        → Update RefundTransaction (COMPLETED)
    → IF insufficient:
        → Update RefundTransaction (FAILED)
        → NotificationService → Send to Admin
```

## Data Models

### Complaint Entity

```java
@Entity
@Table(name = "complaints")
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID complaintId;
    
    // Relationship to Order (nullable for CustomOrder)
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    // Relationship to CustomOrder (nullable for Order)
    @ManyToOne
    @JoinColumn(name = "custom_order_id")
    private CustomOrder customOrder;
    
    // Customer who filed the complaint
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Account customer;
    
    // Artisan responsible for the product
    @ManyToOne(optional = false)
    @JoinColumn(name = "artisan_id")
    private Artisan artisan;
    
    // Complaint details
    @Column(nullable = false, length = 1000)
    private String reason;
    
    @ElementCollection
    @CollectionTable(name = "complaint_images")
    private List<String> evidenceImages = new ArrayList<>();
    
    // Artisan response
    @Column(length = 1000)
    private String artisanResponse;
    
    @Column(nullable = false)
    private Boolean requireReturn = false;
    
    private LocalDateTime artisanResponseAt;
    
    // Admin decision
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.PENDING;
    
    @Column(precision = 18, scale = 2)
    private BigDecimal refundAmount;
    
    @Column(length = 500)
    private String adminNote;
    
    @Column(length = 500)
    private String rejectionReason;
    
    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private Account reviewedBy;
    
    private LocalDateTime reviewedAt;
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Relationship to RefundTransaction
    @OneToOne(mappedBy = "complaint", cascade = CascadeType.ALL)
    private RefundTransaction refundTransaction;
    
    // Relationship to Return Shipment (reuse existing Shipment entity)
    @OneToOne(mappedBy = "complaint", cascade = CascadeType.ALL)
    private Shipment returnShipment;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### ComplaintStatus Enum

```java
public enum ComplaintStatus {
    PENDING,           // Chờ xử lý
    WAITING_RETURN,    // Chờ trả hàng (nếu requireReturn = true)
    PROCESSING_REFUND, // Đang xử lý hoàn tiền
    APPROVED,          // Đã phê duyệt và hoàn tiền thành công
    REJECTED           // Đã từ chối
}
```

### RefundTransaction Entity

```java
@Entity
@Table(name = "refund_transactions")
public class RefundTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID refundTransactionId;
    
    @OneToOne(optional = false)
    @JoinColumn(name = "complaint_id", unique = true)
    private Complaint complaint;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet; // Artisan wallet
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet; // Customer wallet
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status = RefundStatus.PENDING;
    
    @Column(length = 500)
    private String failureReason;
    
    // Reference to WalletTransaction entries
    @OneToOne
    @JoinColumn(name = "debit_transaction_id")
    private WalletTransaction debitTransaction;
    
    @OneToOne
    @JoinColumn(name = "credit_transaction_id")
    private WalletTransaction creditTransaction;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime completedAt;
    
    @PreUpdate
    public void preUpdate() {
        if (status == RefundStatus.COMPLETED || status == RefundStatus.FAILED) {
            this.completedAt = LocalDateTime.now();
        }
    }
}
```

### RefundStatus Enum

```java
public enum RefundStatus {
    PENDING,    // Chờ xử lý
    COMPLETED,  // Hoàn thành
    FAILED      // Thất bại
}
```

### Shipment Entity Extension (Reuse Existing)

**Thay vì tạo entity mới, chúng ta sẽ mở rộng entity Shipment hiện có:**

```java
@Entity
@Table(name = "shipments")
public class Shipment {
    // ... existing fields ...
    
    // NEW FIELDS for return shipment support
    @Column(nullable = false)
    private Boolean isReturn = false;
    
    @ManyToOne
    @JoinColumn(name = "complaint_id")
    private Complaint complaint; // Only set if isReturn = true
    
    // ... rest of existing fields ...
}
```

**Lợi ích:**
- Tái sử dụng logic tracking và status hiện có
- Không cần tạo entity mới
- Dễ dàng query cả shipment thường và return shipment
- Tích hợp với GHN API hiện có

## Components and Interfaces

### ComplaintService Interface

```java
public interface ComplaintService {
    
    /**
     * Create a new complaint for an order
     * @throws IllegalStateException if order is not DELIVERED or outside 7-day window
     * @throws IllegalArgumentException if complaint already exists for this order
     */
    ComplaintResponse createComplaint(CreateComplaintRequest request, UUID customerId);
    
    /**
     * Artisan responds to complaint and decides on return requirement
     */
    ComplaintResponse respondToComplaint(UUID complaintId, ArtisanResponseRequest request, UUID artisanId);
    
    /**
     * Admin approves complaint
     * - If requireReturn = false: Trigger refund immediately
     * - If requireReturn = true: Update status to WAITING_RETURN
     */
    ComplaintResponse approveComplaint(UUID complaintId, ApproveComplaintRequest request, UUID adminId);
    
    /**
     * Admin rejects complaint
     */
    ComplaintResponse rejectComplaint(UUID complaintId, RejectComplaintRequest request, UUID adminId);
    
    /**
     * Get complaint details
     */
    ComplaintDetailResponse getComplaintDetails(UUID complaintId, UUID userId);
    
    /**
     * List complaints for customer
     */
    Page<ComplaintResponse> getCustomerComplaints(UUID customerId, Pageable pageable);
    
    /**
     * List complaints for artisan
     */
    Page<ComplaintResponse> getArtisanComplaints(UUID artisanId, Pageable pageable);
    
    /**
     * List all complaints for admin
     */
    Page<ComplaintResponse> getAllComplaints(ComplaintStatus status, Pageable pageable);
    
    /**
     * Validate if order is eligible for complaint
     */
    boolean isEligibleForComplaint(UUID orderId, UUID customOrderId);
}
```

### RefundService Interface

```java
public interface RefundService {
    
    /**
     * Process refund for approved complaint
     * - Check wallet balance
     * - Deduct from artisan wallet
     * - Add to customer wallet
     * - Create wallet transactions
     * - Update refund transaction status
     * 
     * @throws InsufficientBalanceException if artisan wallet doesn't have enough balance
     */
    RefundTransaction processRefund(Complaint complaint, BigDecimal amount);
    
    /**
     * Retry failed refund (admin action)
     */
    RefundTransaction retryRefund(UUID refundTransactionId, UUID adminId);
    
    /**
     * Get refund transaction details
     */
    RefundTransactionResponse getRefundTransaction(UUID refundTransactionId);
    
    /**
     * List all refund transactions for admin
     */
    Page<RefundTransactionResponse> getAllRefundTransactions(RefundStatus status, Pageable pageable);
}
```

### ShippingService Extension (Reuse Existing)

**Mở rộng ShippingService hiện có để hỗ trợ return shipment:**

```java
public interface ShippingService {
    // ... existing methods ...
    
    /**
     * Customer creates return shipment for complaint
     * - Reuses existing Shipment entity with isReturn = true
     */
    Shipment createReturnShipment(UUID complaintId, CreateShipmentRequest request, UUID customerId);
    
    /**
     * Artisan confirms receipt of returned item
     * - Updates shipment status
     * - Triggers refund process via ComplaintService
     */
    Shipment confirmReturnReceipt(UUID shipmentId, UUID artisanId);
    
    /**
     * Get return shipment for complaint
     */
    Shipment getReturnShipmentByComplaint(UUID complaintId);
}
```

## API Endpoints

### Customer Endpoints

```
POST   /api/complaints                    - Create complaint
GET    /api/complaints                    - List my complaints
GET    /api/complaints/{id}               - Get complaint details
POST   /api/complaints/{id}/return        - Create return shipment (optional)
```

### Artisan Endpoints

```
GET    /api/artisan/complaints            - List complaints for my products
GET    /api/artisan/complaints/{id}       - Get complaint details
POST   /api/artisan/complaints/{id}/respond - Respond to complaint
POST   /api/artisan/return-shipments/{id}/confirm - Confirm receipt (optional)
```

### Admin Endpoints

```
GET    /api/admin/complaints              - List all complaints (with filters)
GET    /api/admin/complaints/{id}         - Get complaint details
POST   /api/admin/complaints/{id}/approve - Approve complaint
POST   /api/admin/complaints/{id}/reject  - Reject complaint
GET    /api/admin/refund-transactions     - List all refund transactions
POST   /api/admin/refund-transactions/{id}/retry - Retry failed refund
```

## Business Logic Implementation

### 1. Create Complaint Logic

```java
@Transactional
public ComplaintResponse createComplaint(CreateComplaintRequest request, UUID customerId) {
    // 1. Validate order exists and belongs to customer
    Order order = null;
    CustomOrder customOrder = null;
    Artisan artisan = null;
    
    if (request.getOrderId() != null) {
        order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new NotFoundException("Order not found"));
        
        if (!order.getCustomer().getAccountId().equals(customerId)) {
            throw new UnauthorizedException("Order does not belong to customer");
        }
        
        // Determine artisan from order details
        artisan = determineArtisanFromOrder(order, request.getProductId());
    } else if (request.getCustomOrderId() != null) {
        customOrder = customOrderRepository.findById(request.getCustomOrderId())
            .orElseThrow(() -> new NotFoundException("Custom order not found"));
        
        if (!customOrder.getRequest().getCustomer().getAccountId().equals(customerId)) {
            throw new UnauthorizedException("Custom order does not belong to customer");
        }
        
        artisan = customOrder.getArtisan();
    } else {
        throw new IllegalArgumentException("Either orderId or customOrderId must be provided");
    }
    
    // 2. Validate order status is DELIVERED
    String orderStatus = order != null ? order.getStatus() : customOrder.getStatus().name();
    if (!"DELIVERED".equals(orderStatus) && !CustomOrderStatus.COMPLETED.name().equals(orderStatus)) {
        throw new IllegalStateException("Can only create complaint for delivered orders");
    }
    
    // 3. Validate within 7-day window
    LocalDateTime deliveredDate = order != null ? order.getUpdateAt() : customOrder.getUpdatedAt();
    if (deliveredDate.plusDays(7).isBefore(LocalDateTime.now())) {
        throw new IllegalStateException("Complaint must be filed within 7 days of delivery");
    }
    
    // 4. Check if complaint already exists
    boolean exists = complaintRepository.existsByOrderOrCustomOrder(
        order != null ? order.getOrderId() : null,
        customOrder != null ? customOrder.getCustomOrderId() : null
    );
    if (exists) {
        throw new IllegalArgumentException("Complaint already exists for this order");
    }
    
    // 5. Create complaint
    Complaint complaint = new Complaint();
    complaint.setOrder(order);
    complaint.setCustomOrder(customOrder);
    complaint.setCustomer(accountRepository.findById(customerId).orElseThrow());
    complaint.setArtisan(artisan);
    complaint.setReason(request.getReason());
    complaint.setEvidenceImages(request.getEvidenceImages());
    complaint.setStatus(ComplaintStatus.PENDING);
    
    complaint = complaintRepository.save(complaint);
    
    // 6. Send notification to artisan
    notificationService.sendComplaintNotification(complaint, artisan.getAccount());
    
    return mapToResponse(complaint);
}
```

### 2. Artisan Response Logic

```java
@Transactional
public ComplaintResponse respondToComplaint(UUID complaintId, ArtisanResponseRequest request, UUID artisanId) {
    // 1. Get complaint and validate
    Complaint complaint = complaintRepository.findById(complaintId)
        .orElseThrow(() -> new NotFoundException("Complaint not found"));
    
    if (!complaint.getArtisan().getAccount().getAccountId().equals(artisanId)) {
        throw new UnauthorizedException("Complaint does not belong to artisan");
    }
    
    if (complaint.getStatus() != ComplaintStatus.PENDING) {
        throw new IllegalStateException("Can only respond to pending complaints");
    }
    
    // 2. Update complaint with response
    complaint.setArtisanResponse(request.getResponse());
    complaint.setRequireReturn(request.getRequireReturn());
    complaint.setArtisanResponseAt(LocalDateTime.now());
    
    complaint = complaintRepository.save(complaint);
    
    // 3. Send notification to admin
    Account platformAdmin = walletService.getPlatformAdminAccount();
    notificationService.sendArtisanResponseNotification(complaint, platformAdmin);
    
    return mapToResponse(complaint);
}
```

### 3. Admin Approve Logic

```java
@Transactional
public ComplaintResponse approveComplaint(UUID complaintId, ApproveComplaintRequest request, UUID adminId) {
    // 1. Get complaint and validate
    Complaint complaint = complaintRepository.findById(complaintId)
        .orElseThrow(() -> new NotFoundException("Complaint not found"));
    
    if (complaint.getStatus() != ComplaintStatus.PENDING) {
        throw new IllegalStateException("Can only approve pending complaints");
    }
    
    // 2. Validate refund amount
    BigDecimal maxRefund = calculateMaxRefundAmount(complaint);
    if (request.getRefundAmount().compareTo(maxRefund) > 0) {
        throw new IllegalArgumentException("Refund amount exceeds maximum allowed");
    }
    
    // 3. Update complaint
    complaint.setRefundAmount(request.getRefundAmount());
    complaint.setAdminNote(request.getAdminNote());
    complaint.setReviewedBy(accountRepository.findById(adminId).orElseThrow());
    complaint.setReviewedAt(LocalDateTime.now());
    
    // 4. Process based on requireReturn flag
    if (complaint.getRequireReturn()) {
        // Require return: Update status to WAITING_RETURN
        complaint.setStatus(ComplaintStatus.WAITING_RETURN);
        complaint = complaintRepository.save(complaint);
        
        // Send notification to customer with return instructions
        notificationService.sendReturnRequiredNotification(complaint);
    } else {
        // No return required: Process refund immediately
        complaint.setStatus(ComplaintStatus.PROCESSING_REFUND);
        complaint = complaintRepository.save(complaint);
        
        try {
            // Process refund
            RefundTransaction refundTransaction = refundService.processRefund(
                complaint, 
                request.getRefundAmount()
            );
            
            // Update complaint status to APPROVED
            complaint.setStatus(ComplaintStatus.APPROVED);
            complaint = complaintRepository.save(complaint);
            
            // Send success notifications
            notificationService.sendRefundSuccessNotification(complaint);
        } catch (InsufficientBalanceException e) {
            // Refund failed due to insufficient balance
            complaint.setStatus(ComplaintStatus.PENDING);
            complaint = complaintRepository.save(complaint);
            
            // Send notification to admin
            notificationService.sendRefundFailedNotification(complaint, e.getMessage());
            
            throw e;
        }
    }
    
    return mapToResponse(complaint);
}
```

### 4. Process Refund Logic

```java
@Transactional
public RefundTransaction processRefund(Complaint complaint, BigDecimal amount) {
    // 1. Get wallets
    Wallet artisanWallet = walletService.getOrCreateWallet(complaint.getArtisan().getAccount());
    Wallet customerWallet = walletService.getOrCreateWallet(complaint.getCustomer());
    
    // 2. Check artisan wallet balance
    if (artisanWallet.getBalance().compareTo(amount) < 0) {
        // Insufficient balance: Create failed refund transaction
        RefundTransaction refundTransaction = new RefundTransaction();
        refundTransaction.setComplaint(complaint);
        refundTransaction.setAmount(amount);
        refundTransaction.setFromWallet(artisanWallet);
        refundTransaction.setToWallet(customerWallet);
        refundTransaction.setStatus(RefundStatus.FAILED);
        refundTransaction.setFailureReason("Insufficient balance in artisan wallet");
        
        refundTransactionRepository.save(refundTransaction);
        
        throw new InsufficientBalanceException("Artisan wallet does not have sufficient balance");
    }
    
    // 3. Create refund transaction
    RefundTransaction refundTransaction = new RefundTransaction();
    refundTransaction.setComplaint(complaint);
    refundTransaction.setAmount(amount);
    refundTransaction.setFromWallet(artisanWallet);
    refundTransaction.setToWallet(customerWallet);
    refundTransaction.setStatus(RefundStatus.PENDING);
    
    refundTransaction = refundTransactionRepository.save(refundTransaction);
    
    // 4. Deduct from artisan wallet
    WalletTransaction debitTx = new WalletTransaction();
    debitTx.setWallet(artisanWallet);
    debitTx.setAmount(amount.negate());
    debitTx.setType(TransactionType.REFUND_DEBIT);
    debitTx.setDescription("Refund for complaint #" + complaint.getComplaintId());
    debitTx.setRelatedEntityType(RelatedEntityType.COMPLAINT);
    debitTx.setRelatedEntityId(complaint.getComplaintId());
    
    debitTx = walletTransactionRepository.save(debitTx);
    
    // Update artisan wallet balance
    artisanWallet.setBalance(artisanWallet.getBalance().subtract(amount));
    walletRepository.save(artisanWallet);
    
    // 5. Add to customer wallet
    WalletTransaction creditTx = new WalletTransaction();
    creditTx.setWallet(customerWallet);
    creditTx.setAmount(amount);
    creditTx.setType(TransactionType.REFUND_CREDIT);
    creditTx.setDescription("Refund for complaint #" + complaint.getComplaintId());
    creditTx.setRelatedEntityType(RelatedEntityType.COMPLAINT);
    creditTx.setRelatedEntityId(complaint.getComplaintId());
    
    creditTx = walletTransactionRepository.save(creditTx);
    
    // Update customer wallet balance
    customerWallet.setBalance(customerWallet.getBalance().add(amount));
    walletRepository.save(customerWallet);
    
    // 6. Update refund transaction
    refundTransaction.setDebitTransaction(debitTx);
    refundTransaction.setCreditTransaction(creditTx);
    refundTransaction.setStatus(RefundStatus.COMPLETED);
    refundTransaction.setCompletedAt(LocalDateTime.now());
    
    return refundTransactionRepository.save(refundTransaction);
}
```

### 5. Calculate Max Refund Amount

```java
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
```

## Error Handling

### Custom Exceptions

```java
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

public class ComplaintNotEligibleException extends RuntimeException {
    public ComplaintNotEligibleException(String message) {
        super(message);
    }
}

public class ComplaintAlreadyExistsException extends RuntimeException {
    public ComplaintAlreadyExistsException(String message) {
        super(message);
    }
}
```

### Global Exception Handler

```java
@ExceptionHandler(InsufficientBalanceException.class)
public ResponseEntity<BaseResponse> handleInsufficientBalance(InsufficientBalanceException e) {
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(BaseResponse.error("INSUFFICIENT_BALANCE", e.getMessage()));
}

@ExceptionHandler(ComplaintNotEligibleException.class)
public ResponseEntity<BaseResponse> handleComplaintNotEligible(ComplaintNotEligibleException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(BaseResponse.error("COMPLAINT_NOT_ELIGIBLE", e.getMessage()));
}

@ExceptionHandler(ComplaintAlreadyExistsException.class)
public ResponseEntity<BaseResponse> handleComplaintAlreadyExists(ComplaintAlreadyExistsException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(BaseResponse.error("COMPLAINT_ALREADY_EXISTS", e.getMessage()));
}
```

## Testing Strategy

### Unit Tests

**ComplaintServiceTest:**
- Test create complaint with valid order
- Test create complaint with invalid order status
- Test create complaint outside 7-day window
- Test create complaint when one already exists
- Test artisan response
- Test admin approve without return
- Test admin approve with return
- Test admin reject

**RefundServiceTest:**
- Test process refund with sufficient balance
- Test process refund with insufficient balance
- Test calculate max refund amount
- Test retry failed refund

### Integration Tests

**ComplaintIntegrationTest:**
- Test full complaint flow: create → artisan respond → admin approve → refund
- Test complaint with return flow: create → artisan respond → admin approve → customer ship → artisan confirm → refund
- Test concurrent complaint creation for same order
- Test wallet balance updates after refund

### API Tests

**ComplaintControllerTest:**
- Test all endpoints with valid/invalid data
- Test authorization (customer can only see their complaints, etc.)
- Test pagination and filtering

## Security Considerations

1. **Authorization:**
   - Customer can only create complaints for their own orders
   - Artisan can only respond to complaints for their products
   - Admin has full access to all complaints

2. **Data Validation:**
   - Validate image URLs are from trusted storage (Supabase)
   - Sanitize text inputs to prevent XSS
   - Validate refund amount doesn't exceed maximum

3. **Transaction Safety:**
   - Use @Transactional for all money-related operations
   - Implement optimistic locking on Wallet entity
   - Log all refund transactions for audit

4. **Rate Limiting:**
   - Limit complaint creation to prevent spam
   - Limit image uploads per complaint

## Performance Considerations

1. **Database Indexes:**
   ```sql
   CREATE INDEX idx_complaint_customer ON complaints(customer_id);
   CREATE INDEX idx_complaint_artisan ON complaints(artisan_id);
   CREATE INDEX idx_complaint_status ON complaints(status);
   CREATE INDEX idx_complaint_order ON complaints(order_id);
   CREATE INDEX idx_complaint_custom_order ON complaints(custom_order_id);
   CREATE INDEX idx_refund_status ON refund_transactions(status);
   ```

2. **Caching:**
   - Cache platform admin account
   - Cache complaint eligibility checks (7-day window)

3. **Async Processing:**
   - Send notifications asynchronously
   - Process image uploads asynchronously

## Deployment Considerations

1. **Database Migration:**
   - Create new tables: complaints, refund_transactions, return_shipments
   - Add new enum values to existing enums if needed
   - Create indexes

2. **Backward Compatibility:**
   - New feature, no breaking changes to existing APIs

3. **Monitoring:**
   - Monitor refund transaction success/failure rates
   - Alert on high complaint rates for specific artisans
   - Track average complaint resolution time

## Future Enhancements

1. **Automated Return Tracking:**
   - Integrate with GHN API for return shipment tracking
   - Auto-trigger refund when shipment is delivered

2. **Dispute Resolution:**
   - Allow artisan to dispute admin decision
   - Implement escalation process

3. **Partial Refunds:**
   - Support partial refunds for partially damaged items

4. **Refund Analytics:**
   - Dashboard showing refund trends
   - Artisan performance metrics based on complaint rates
