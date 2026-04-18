# Implementation Plan - Hệ Thống Hoàn Tiền

## Task List

- [x] 1. Tạo các Entity và Enum cơ bản
  - Tạo entity Complaint với các field cần thiết
  - Tạo entity RefundTransaction
  - Tạo enum ComplaintStatus (PENDING, WAITING_RETURN, PROCESSING_REFUND, APPROVED, REJECTED)
  - Tạo enum RefundStatus (PENDING, COMPLETED, FAILED)
  - Mở rộng entity Shipment với field isReturn và complaint
  - _Requirements: 1.1, 1.6, 4.1, 4.5, 4.6, 4.7, 4.8_

- [x] 2. Tạo Repository và DTO
  - [x] 2.1 Tạo ComplaintRepository với các query method cần thiết
    - Tạo interface ComplaintRepository extends JpaRepository
    - Thêm method existsByOrderOrCustomOrder để check complaint đã tồn tại
    - Thêm method findByCustomer để lấy danh sách complaint của customer
    - Thêm method findByArtisan để lấy danh sách complaint của artisan
    - Thêm method findByStatus để filter theo trạng thái
    - _Requirements: 11.2, 7.1, 8.1_

  - [x] 2.2 Tạo RefundTransactionRepository




    - Tạo interface RefundTransactionRepository extends JpaRepository
    - Thêm method findByComplaint để lấy refund transaction theo complaint
    - Thêm method findByStatus để filter theo trạng thái
    - _Requirements: 9.1_
  - [x] 2.3 Tạo các DTO Request




  - [x] 2.3 Tạo các DTO Request
    - Tạo CreateComplaintRequest (orderId, customOrderId, reason, evidenceImages)
    - Tạo ArtisanResponseRequest (response, requireReturn)
    - Tạo ApproveComplaintRequest (refundAmount, adminNote)
    - Tạo RejectComplaintRequest (rejectionReason)
    - Thêm validation annotations (@NotNull, @Size, @Min, @Max)
    - _Requirements: 1.2, 1.3, 2.3, 2.4, 2.5, 3.4, 3.5, 3.6_
  - [x] 2.4 Tạo các DTO Response



  - [x] 2.4 Tạo các DTO Response
    - Tạo ComplaintResponse với các field cơ bản
    - Tạo ComplaintDetailResponse với đầy đủ thông tin (bao gồm artisan response, admin decision)
    - Tạo RefundTransactionResponse
    - _Requirements: 7.2, 7.3, 7.4, 7.5_

- [x] 3. Implement ComplaintService - Core Logic
  - [x] 3.1 Implement createComplaint method




    - Validate order/customOrder tồn tại và thuộc về customer
    - Validate order status là DELIVERED hoặc COMPLETED
    - Validate trong vòng 7 ngày kể từ deliveredDate
    - Check complaint đã tồn tại cho order này chưa
    - Xác định Artisan từ order/customOrder
    - Tạo Complaint entity với status PENDING
    - Gửi notification đến Artisan
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 10.1, 10.2, 10.3, 10.4, 11.2_

  - [x] 3.2 Implement respondToComplaint method




    - Validate complaint tồn tại và thuộc về artisan
    - Validate complaint status là PENDING
    - Update complaint với artisanResponse và requireReturn
    - Lưu artisanResponseAt timestamp
    - Gửi notification đến Admin
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

  - [x] 3.3 Implement approveComplaint method




    - Validate complaint status là PENDING
    - Validate refundAmount không vượt quá max (90% của total)
    - Update complaint với refundAmount, adminNote, reviewedBy, reviewedAt
    - Nếu requireReturn = false: Gọi RefundService.processRefund và update status thành APPROVED
    - Nếu requireReturn = true: Update status thành WAITING_RETURN
    - Gửi notification đến Customer và Artisan
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.7, 4.1, 4.2, 6.1, 6.2, 6.3_
-

  - [x] 3.4 Implement rejectComplaint method


    - Validate complaint status là PENDING
    - Update complaint với rejectionReason, reviewedBy, reviewedAt, status REJECTED
    - Gửi notification đến Customer
    - _Requirements: 3.6, 3.7, 6.3_


  - [x] 3.5 Implement query methods



    - Implement getComplaintDetails với authorization check
    - Implement getCustomerComplaints với pagination
    - Implement getArtisanComplaints với pagination
    - Implement getAllComplaints với filter theo status
    - _Requirements: 7.1, 7.2, 8.1, 8.2, 3.1_

  - [x] 3.6 Implement helper methods




    - Implement calculateMaxRefundAmount (90% của order total)
    - Implement determineArtisanFromOrder (xử lý cả Order và CustomOrder)
    - Implement isEligibleForComplaint (check DELIVERED và 7 days)
    - _Requirements: 10.5, 10.6, 10.3, 10.4, 1.4, 1.5_

- [x] 4. Implement RefundService - Xử Lý Hoàn Tiền
  - [x] 4.1 Implement processRefund method



    - Lấy wallet của Artisan và Customer
    - Check số dư wallet Artisan có đủ không
    - Nếu không đủ: Tạo RefundTransaction với status FAILED, gửi notification đến Admin, throw exception
    - Nếu đủ: Tạo RefundTransaction với status PENDING
    - Tạo WalletTransaction debit cho Artisan (amount âm, type REFUND_DEBIT)
    - Update balance của Artisan wallet
    - Tạo WalletTransaction credit cho Customer (amount dương, type REFUND_CREDIT)
    - Update balance của Customer wallet
    - Update RefundTransaction với debitTransaction, creditTransaction, status COMPLETED
    - Sử dụng @Transactional để đảm bảo atomicity
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 11.4, 11.5_
-

  - [x] 4.2 Implement retryRefund method (Admin action)


    - Validate refund transaction tồn tại và có status FAILED
    - Gọi lại processRefund với complaint và amount từ failed transaction
    - _Requirements: 11.5_

  - [x] 4.3 Implement query methods
    - Implement getRefundTransaction
    - Implement getAllRefundTransactions với filter theo status
    - _Requirements: 9.1_

- [x] 5. Mở Rộng ShippingService - Return Shipment (Optional)
  - [x] 5.1 Implement createReturnShipment method
    - Validate complaint status là WAITING_RETURN
    - Validate complaint thuộc về customer
    - Tạo Shipment với isReturn = true và complaint reference
    - Lưu tracking number và shipping provider
    - Gọi GHN API để tạo đơn vận chuyển (nếu cần)
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 5.2 Implement confirmReturnReceipt method
    - Validate shipment tồn tại và isReturn = true
    - Validate shipment thuộc về artisan (qua complaint)
    - Update shipment status
    - Update complaint status từ WAITING_RETURN sang PROCESSING_REFUND
    - Gọi RefundService.processRefund để xử lý hoàn tiền
    - Update complaint status thành APPROVED sau khi refund thành công
    - _Requirements: 5.4, 5.5, 5.6_

  - [x] 5.3 Implement getReturnShipmentByComplaint method
    - Query shipment với complaint và isReturn = true
    - _Requirements: 5.6_

- [x] 6. Thêm TransactionType mới cho Wallet
  - Thêm REFUND_DEBIT và REFUND_CREDIT vào enum WalletTransactionType
  - _Requirements: 4.4, 4.5_

- [x] 7. Thêm NotificationType cho Complaint
  - Thêm COMPLAINT_CREATED, ARTISAN_RESPONDED, COMPLAINT_APPROVED, COMPLAINT_REJECTED, REFUND_COMPLETED, REFUND_FAILED vào enum NotificationType
  - _Requirements: 1.7, 2.1, 6.1, 6.2, 6.3, 6.4_

- [x] 8. Tạo Custom Exceptions
  - Tạo InsufficientBalanceException
  - Thêm handler trong GlobalExceptionHandler
  - _Requirements: 4.7, 11.1_

- [x] 9. Tạo Controller và API Endpoints




- [-] 9. Tạo Controller và API Endpoints

  - [x] 9.1 Tạo ComplaintController - Customer endpoints

    - POST /api/complaints - createComplaint
    - GET /api/complaints - getMyComplaints (pagination)
    - GET /api/complaints/{id} - getComplaintDetails
    - POST /api/complaints/{id}/return - createReturnShipment (optional)
    - Thêm @PreAuthorize cho CUSTOMER role
    - Thêm validation và error handling
    - _Requirements: 1.1-1.7, 7.1-7.5_


  - [x] 9.2 Tạo ArtisanComplaintController - Artisan endpoints

    - GET /api/artisan/complaints - getMyComplaints (pagination)
    - GET /api/artisan/complaints/{id} - getComplaintDetails
    - POST /api/artisan/complaints/{id}/respond - respondToComplaint
    - POST /api/artisan/return-shipments/{id}/confirm - confirmReturnReceipt (optional)
    - Thêm @PreAuthorize cho ARTISAN role
    - _Requirements: 2.1-2.8, 8.1-8.3_

  - [x] 9.3 Tạo AdminComplaintController - Admin endpoints


    - GET /api/admin/complaints - getAllComplaints (pagination, filter by status)
    - GET /api/admin/complaints/{id} - getComplaintDetails
    - POST /api/admin/complaints/{id}/approve - approveComplaint
    - POST /api/admin/complaints/{id}/reject - rejectComplaint
    - GET /api/admin/refund-transactions - getAllRefundTransactions
    - POST /api/admin/refund-transactions/{id}/retry - retryRefund
    - Thêm @PreAuthorize cho ADMIN role
    - _Requirements: 3.1-3.7, 9.1, 9.2_

- [ ]* 10. Testing (Optional - Có thể bỏ qua trong MVP)
  - [ ]* 10.1 Unit tests cho ComplaintService
    - Test createComplaint với các trường hợp: valid, invalid status, outside 7 days, duplicate
    - Test respondToComplaint
    - Test approveComplaint với requireReturn = true/false
    - Test rejectComplaint
    - _Requirements: All_

  - [ ]* 10.2 Unit tests cho RefundService
    - Test processRefund với sufficient balance
    - Test processRefund với insufficient balance
    - Test calculateMaxRefundAmount
    - _Requirements: 4.1-4.8_

  - [ ]* 10.3 Integration tests
    - Test full flow: create → respond → approve → refund
    - Test flow with return: create → respond → approve → ship → confirm → refund
    - Test concurrent complaint creation
    - _Requirements: All_

- [ ]* 11. Documentation (Optional)
  - [ ]* 11.1 API documentation
    - Thêm Swagger annotations cho tất cả endpoints
    - Thêm example request/response
    - _Requirements: All_

  - [ ]* 11.2 README
    - Viết hướng dẫn sử dụng API
    - Viết business rules và flow diagram
    - _Requirements: All_
