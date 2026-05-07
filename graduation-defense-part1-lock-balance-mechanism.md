# PHẦN 1: GIẢI THÍCH CƠ CHẾ LOCK BALANCE & WITHDRAWAL

## 1. TỔNG QUAN HỆ THỐNG VÍ ĐIỆN TỬ (WALLET)

### 1.1. Kiến trúc Wallet
```
Wallet {
    - walletId: UUID
    - account: Account (1-1 relationship)
    - balance: BigDecimal (Tổng số dư)
    - lockedBalance: BigDecimal (Số dư bị khóa)
    - createdAt, updatedAt: LocalDateTime
    
    + getAvailableBalance(): BigDecimal
      → return balance - lockedBalance
}
```

**Công thức quan trọng:**
```
Available Balance (Số dư khả dụng) = Total Balance - Locked Balance
```

### 1.2. Ai có Wallet?
- **ARTISAN**: Có wallet để nhận tiền từ đơn hàng
- **ADMIN**: Có wallet để nhận phí hoa hồng (commission)
- **CUSTOMER**: KHÔNG có wallet (hoàn tiền qua VNPay)

---

## 2. CƠ CHẾ LOCK BALANCE (KHÓA SỐ DƯ)

### 2.1. Tại sao cần Lock Balance?

**Vấn đề thực tế:**
Trong Custom Order (đơn hàng tùy chỉnh), khách hàng thanh toán theo từng giai đoạn (stages). Nếu artisan nhận 100% tiền ngay sau khi hoàn thành stage và rút hết, khi khách hàng khiếu nại về chất lượng sản phẩm cuối cùng, sẽ không có tiền để hoàn lại.

**Giải pháp:**
Áp dụng cơ chế **70/30 Split**:
- **70%** số tiền: Artisan có thể rút ngay
- **30%** số tiền: Bị khóa (locked) trong 3 ngày sau khi hoàn thành stage

### 2.2. Luồng hoạt động Lock Balance

#### Bước 1: Customer thanh toán Stage Payment
```
Ví dụ: Stage 1 = 10,000,000 VND
```

#### Bước 2: Hệ thống tính toán phân phối
```java
// Trong StagePaymentServiceImp.java

BigDecimal totalAmount = 10,000,000 VND

// 1. Trừ Platform Fee (10%)
BigDecimal platformFee = totalAmount × 0.10 = 1,000,000 VND
BigDecimal artisanAmountBeforeCommission = 10,000,000 - 1,000,000 = 9,000,000 VND

// 2. Trừ Commission (ví dụ 5%)
BigDecimal commissionFee = 9,000,000 × 0.05 = 450,000 VND
BigDecimal artisanNetAmount = 9,000,000 - 450,000 = 8,550,000 VND

// 3. Tính 70/30 split
BigDecimal lockedAmount = 8,550,000 × 0.30 = 2,565,000 VND (LOCKED)
BigDecimal availableAmount = 8,550,000 × 0.70 = 5,985,000 VND (AVAILABLE)
```

#### Bước 3: Cập nhật Wallet
```java
// Trong WalletServiceImp.depositToWalletWithLock()

Wallet wallet = artisan.getWallet();

// Cộng TOÀN BỘ vào balance
wallet.balance = oldBalance + 8,550,000 VND

// Cộng phần LOCKED vào lockedBalance
wallet.lockedBalance = oldLockedBalance + 2,565,000 VND

// Số dư khả dụng
availableBalance = balance - lockedBalance
                 = 8,550,000 - 2,565,000 
                 = 5,985,000 VND (có thể rút)
```

### 2.3. Khi nào Lock Balance được giải phóng?

**Điều kiện:**
- Stage đã hoàn thành (status = COMPLETED)
- Đã qua 3 ngày kể từ khi hoàn thành
- Không có khiếu nại đang chờ xử lý

**Cơ chế tự động:**
```java
// LockedBalanceReleaseScheduler.java
@Scheduled(cron = "0 0 2 * * *") // Chạy lúc 2h sáng mỗi ngày
public void releaseLockedBalances() {
    // 1. Tìm các stage đã COMPLETED > 3 ngày
    LocalDateTime releaseDate = LocalDateTime.now().minusDays(3);
    List<CustomOrderStage> eligibleStages = 
        stageRepository.findCompletedStagesForBalanceRelease(
            StageStatus.COMPLETED, 
            releaseDate
        );
    
    // 2. Với mỗi stage
    for (CustomOrderStage stage : eligibleStages) {
        // Tính số tiền bị khóa (30% của stage amount)
        BigDecimal lockedAmount = stage.getAmount() × 0.30;
        
        // Trừ khỏi lockedBalance
        wallet.lockedBalance = wallet.lockedBalance - lockedAmount;
        
        // Đánh dấu đã giải phóng
        stage.balanceReleased = true;
    }
}
```

**Kết quả:**
- `lockedBalance` giảm xuống
- `availableBalance` tăng lên
- Artisan có thể rút thêm tiền

---

## 3. CƠ CHẾ WITHDRAWAL (RÚT TIỀN)

### 3.1. Quy trình Withdrawal

```
┌─────────────┐
│   ARTISAN   │
│  Tạo yêu cầu│
│   rút tiền  │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│ VALIDATION (WithdrawalServiceImp)  │
├─────────────────────────────────────┤
│ 1. Kiểm tra role = ARTISAN          │
│ 2. Kiểm tra account verified        │
│ 3. Kiểm tra không có complaint      │
│    đang xử lý (withdrawalFrozen)    │
│ 4. Kiểm tra số dư khả dụng đủ       │
│ 5. Kiểm tra không có withdrawal     │
│    PENDING khác                      │
│ 6. Kiểm tra số tiền: 50K - 50M VND  │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────┐
│ Tạo Withdrawal  │
│ Request         │
│ Status: PENDING │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│ Gửi thông báo   │
│ cho ADMIN       │
│ (Priority: HIGH)│
└──────┬──────────┘
       │
       ▼
┌─────────────────────────────────────┐
│         ADMIN XỬ LÝ                 │
├─────────────────────────────────────┤
│ Option 1: APPROVE                   │
│  - Trừ tiền từ wallet.balance       │
│  - Tạo WalletTransaction (WITHDRAW) │
│  - Chuyển status = APPROVED         │
│  - Gửi thông báo cho Artisan        │
│                                     │
│ Option 2: REJECT                    │
│  - Không trừ tiền                   │
│  - Chuyển status = REJECTED         │
│  - Ghi lý do từ chối                │
│  - Gửi thông báo cho Artisan        │
└─────────────────────────────────────┘
```

### 3.2. Code chi tiết - Tạo Withdrawal Request

```java
// WithdrawalServiceImp.createWithdrawalRequest()

public WithdrawalResponse createWithdrawalRequest(
    UUID artisanId, 
    CreateWithdrawalRequest request
) {
    // 1. Validate artisan
    Artisan artisan = artisanRepository.findById(artisanId)
        .orElseThrow(() -> new NotFoundException("Artisan not found"));
    
    // 2. Kiểm tra có complaint đang xử lý không
    boolean hasActiveComplaint = 
        complaintRepository.hasActiveComplaintWithWithdrawalFrozen(artisanId);
    
    if (hasActiveComplaint) {
        throw new BadRequestException(
            "Không thể rút tiền khi có khiếu nại đang được xử lý"
        );
    }
    
    // 3. Kiểm tra số dư khả dụng
    Wallet wallet = walletRepository.findByAccount(artisan.getAccount())
        .orElseThrow(() -> new NotFoundException("Wallet not found"));
    
    BigDecimal availableBalance = wallet.getAvailableBalance();
    // availableBalance = balance - lockedBalance
    
    if (availableBalance.compareTo(request.getAmount()) < 0) {
        throw new InsufficientBalanceException(
            String.format(
                "Số dư khả dụng: %s VND, " +
                "Số dư bị khóa: %s VND, " +
                "Số tiền yêu cầu: %s VND",
                availableBalance, 
                wallet.getLockedBalance(), 
                request.getAmount()
            )
        );
    }
    
    // 4. Kiểm tra không có withdrawal PENDING khác
    boolean hasPending = withdrawalRequestRepository
        .existsByArtisan_ArtisanUuidAndStatus(
            artisanId, 
            WithdrawalStatus.PENDING
        );
    
    if (hasPending) {
        throw new PendingWithdrawalExistsException(
            "Bạn đã có yêu cầu rút tiền đang chờ xử lý"
        );
    }
    
    // 5. Tạo withdrawal request
    WithdrawalRequest withdrawal = new WithdrawalRequest();
    withdrawal.setArtisan(artisan);
    withdrawal.setAmount(request.getAmount());
    withdrawal.setStatus(WithdrawalStatus.PENDING);
    withdrawal.setBankName(request.getBankName());
    withdrawal.setBankAccountNumber(request.getBankAccountNumber());
    withdrawal.setBankAccountName(request.getBankAccountName());
    
    withdrawal = withdrawalRequestRepository.save(withdrawal);
    
    // 6. Gửi thông báo cho Admin
    notificationService.notifyAdminOfWithdrawalRequest(
        withdrawal.getWithdrawalId(),
        artisan.getAccount().getFullName(),
        request.getAmount().longValue()
    );
    
    return mapToResponse(withdrawal);
}
```

### 3.3. Code chi tiết - Admin Approve Withdrawal

```java
// WithdrawalServiceImp.approveWithdrawal()

@Transactional
public WithdrawalResponse approveWithdrawal(
    UUID adminId, 
    UUID withdrawalId, 
    ApproveWithdrawalRequest request
) {
    // 1. Lock withdrawal record (pessimistic lock)
    WithdrawalRequest withdrawal = 
        withdrawalRequestRepository.findByIdWithLock(withdrawalId)
            .orElseThrow(() -> new NotFoundException("Not found"));
    
    // 2. Validate status = PENDING
    if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
        throw new InvalidStatusException("Chỉ có thể phê duyệt PENDING");
    }
    
    // 3. Kiểm tra lại số dư khả dụng
    Wallet wallet = walletRepository.findByAccount(
        withdrawal.getArtisan().getAccount()
    ).orElseThrow(() -> new NotFoundException("Wallet not found"));
    
    BigDecimal availableBalance = wallet.getAvailableBalance();
    
    if (availableBalance.compareTo(withdrawal.getAmount()) < 0) {
        throw new InsufficientBalanceException("Số dư không đủ");
    }
    
    // 4. TRỪ TIỀN TỪ WALLET
    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.subtract(withdrawal.getAmount());
    wallet.setBalance(balanceAfter);
    walletRepository.save(wallet);
    
    // 5. Tạo WalletTransaction
    WalletTransaction transaction = new WalletTransaction();
    transaction.setWallet(wallet);
    transaction.setType(WalletTransactionType.WITHDRAW);
    transaction.setAmount(withdrawal.getAmount());
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setDescription(
        String.format("Rút tiền về tài khoản %s - %s",
            bankAccountUtil.mask(withdrawal.getBankAccountNumber()), 
            withdrawal.getBankName()
        )
    );
    
    transaction = walletTransactionRepository.save(transaction);
    
    // 6. Cập nhật withdrawal status
    withdrawal.setStatus(WithdrawalStatus.APPROVED);
    withdrawal.setProcessedBy(admin);
    withdrawal.setProcessedAt(LocalDateTime.now());
    withdrawal.setWalletTransaction(transaction);
    
    withdrawal = withdrawalRequestRepository.save(withdrawal);
    
    // 7. Gửi thông báo cho Artisan
    notificationService.notifyArtisanOfWithdrawalApproval(
        withdrawal.getArtisan().getAccount().getAccountId(),
        withdrawal.getWithdrawalId(),
        withdrawal.getAmount().longValue()
    );
    
    return mapToResponse(withdrawal);
}
```

---

## 4. TÍCH HỢP VỚI COMPLAINT (KHIẾU NẠI)

### 4.1. Withdrawal Freeze khi có Complaint

```java
// Complaint.java
@Column(name = "withdrawal_frozen", nullable = false)
private Boolean withdrawalFrozen = false;
```

**Khi nào withdrawalFrozen = true?**
- Khi khách hàng tạo complaint về đơn hàng của artisan
- Admin đánh dấu complaint cần điều tra

**Ảnh hưởng:**
```java
// WithdrawalServiceImp.createWithdrawalRequest()

boolean hasActiveComplaint = 
    complaintRepository.hasActiveComplaintWithWithdrawalFrozen(artisanId);

if (hasActiveComplaint) {
    throw new BadRequestException(
        "Không thể rút tiền khi có khiếu nại đang được xử lý"
    );
}
```

### 4.2. Khi nào được rút tiền lại?
- Admin giải quyết complaint (RESOLVED hoặc REJECTED)
- withdrawalFrozen = false
- Artisan có thể tạo withdrawal request mới

---

## 5. SƠ ĐỒ TỔNG QUAN

```
┌─────────────────────────────────────────────────────────────┐
│                    CUSTOMER THANH TOÁN                      │
│                    Stage Payment: 10M VND                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              PHÂN PHỐI TIỀN (Distribution)                  │
├─────────────────────────────────────────────────────────────┤
│ 1. Platform Fee (10%):     1,000,000 VND → Admin Wallet    │
│ 2. Commission (5%):          450,000 VND → Admin Wallet    │
│ 3. Artisan Net Amount:     8,550,000 VND                    │
│    ├─ Available (70%):     5,985,000 VND                    │
│    └─ Locked (30%):        2,565,000 VND                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    ARTISAN WALLET                           │
├─────────────────────────────────────────────────────────────┤
│ balance:          8,550,000 VND                             │
│ lockedBalance:    2,565,000 VND                             │
│ availableBalance: 5,985,000 VND (có thể rút)               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              ARTISAN TẠO WITHDRAWAL REQUEST                 │
│              Amount: 5,000,000 VND                          │
├─────────────────────────────────────────────────────────────┤
│ ✓ availableBalance (5,985,000) >= amount (5,000,000)       │
│ ✓ Không có complaint đang xử lý                             │
│ ✓ Không có withdrawal PENDING khác                          │
│ → Status: PENDING                                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    ADMIN APPROVE                            │
├─────────────────────────────────────────────────────────────┤
│ wallet.balance:          8,550,000 - 5,000,000             │
│                        = 3,550,000 VND                      │
│ wallet.lockedBalance:    2,565,000 VND (không đổi)         │
│ wallet.availableBalance: 3,550,000 - 2,565,000             │
│                        = 985,000 VND                        │
│ → Status: APPROVED                                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         SAU 3 NGÀY - STAGE COMPLETED                        │
│         LockedBalanceReleaseScheduler chạy                  │
├─────────────────────────────────────────────────────────────┤
│ wallet.lockedBalance:    2,565,000 - 2,565,000 = 0 VND     │
│ wallet.availableBalance: 3,550,000 - 0 = 3,550,000 VND     │
│ → Artisan có thể rút thêm 3,550,000 VND                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. CÁC TRƯỜNG HỢP ĐẶC BIỆT

### 6.1. Nếu có nhiều Stage?
```
Stage 1: 10M → 70% available (5.985M), 30% locked (2.565M)
Stage 2: 15M → 70% available (8.977M), 30% locked (3.848M)

Total:
- balance: 21.405M
- lockedBalance: 6.413M (2.565M + 3.848M)
- availableBalance: 14.992M

Sau 3 ngày Stage 1 completed:
- lockedBalance: 3.848M (chỉ còn của Stage 2)
- availableBalance: 17.557M
```

### 6.2. Nếu Artisan rút một phần?
```
Trước rút:
- balance: 21.405M
- lockedBalance: 6.413M
- availableBalance: 14.992M

Rút 10M:
- balance: 11.405M (21.405M - 10M)
- lockedBalance: 6.413M (không đổi)
- availableBalance: 4.992M (11.405M - 6.413M)
```

### 6.3. Nếu có Complaint?
```
Complaint được tạo:
- withdrawalFrozen = true
- Artisan KHÔNG THỂ tạo withdrawal request mới
- Withdrawal request đang PENDING vẫn có thể được Admin xử lý

Complaint được giải quyết:
- withdrawalFrozen = false
- Artisan có thể rút tiền lại
```

---

## 7. BẢO MẬT & TÍNH TOÀN VẸN DỮ LIỆU

### 7.1. Pessimistic Locking
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<WithdrawalRequest> findByIdWithLock(UUID id);
```
Đảm bảo không có 2 admin approve cùng 1 withdrawal request.

### 7.2. Optimistic Locking
```java
@Version
private Long version;
```
Phát hiện conflict khi cập nhật withdrawal request.

### 7.3. Transaction Management
```java
@Transactional
public WithdrawalResponse approveWithdrawal(...) {
    // Tất cả operations trong 1 transaction
    // Nếu có lỗi → rollback toàn bộ
}
```

### 7.4. Audit Logging
```java
auditLogger.info(
    "WITHDRAWAL_APPROVED: withdrawalId={}, adminId={}, amount={}", 
    withdrawalId, adminId, amount
);
```

---

## 8. KẾT LUẬN

**Lock Balance** và **Withdrawal** là 2 cơ chế quan trọng để:
1. Bảo vệ quyền lợi khách hàng (giữ 30% trong 3 ngày)
2. Đảm bảo artisan vẫn có tiền để hoạt động (70% rút ngay)
3. Kiểm soát rủi ro khi có khiếu nại (withdrawal freeze)
4. Minh bạch trong quản lý tài chính (audit log, transaction history)

Hệ thống được thiết kế với nhiều lớp validation và bảo mật để đảm bảo tính toàn vẹn dữ liệu và công bằng cho tất cả các bên.
