# Luồng Hoàn Tiền Custom Order với Locked Balance

## Vấn đề
Khi artisan mới có ví 0đ, nhận thanh toán từ custom order stage:
- Hệ thống lock 30% số tiền (để bảo vệ khỏi rút tiền trước khi hoàn thành)
- Nếu customer hủy đơn ngay sau khi thanh toán, artisan cần hoàn tiền
- Nhưng tiền đã bị locked, `availableBalance` không đủ để refund

## Giải pháp đã implement

### 1. Auto-unlock Locked Balance
Trong `CancellationServiceImp`, khi cancel order:
- Kiểm tra nếu `availableBalance` không đủ nhưng `totalBalance` đủ
- Tự động unlock số tiền cần thiết từ `lockedBalance`
- Đảm bảo không unlock quá số tiền đang locked

### 2. Tạo WalletTransaction
Mỗi lần trừ tiền từ ví artisan để refund:
- Tạo `WalletTransaction` với type `REFUND_DEBIT`
- Amount là số âm (negative) để hiển thị là trừ tiền
- Description chi tiết: gross amount, commission, net amount
- Artisan có thể xem lịch sử giao dịch đầy đủ

### 3. Estimate Refund chính xác
Trong `RefundCalculationServiceImp`:
- Kiểm tra `totalBalance` thay vì chỉ `availableBalance`
- Hiển thị đúng khả năng refund của artisan

## Ví dụ Flow

**Artisan mới, ví 0đ:**

1. Customer thanh toán stage: 1,000,000 VND
   - Commission 10%: 100,000 VND
   - Artisan nhận: 900,000 VND (lock 30% = 270,000 VND)
   - Wallet: balance=900k, locked=270k, available=630k

2. Customer hủy đơn
   - Cần refund: 900,000 VND
   - Available (630k) < refund (900k) ❌
   - Total (900k) >= refund (900k) ✅

3. Hệ thống unlock tự động
   - Unlock: 270,000 VND
   - Wallet: balance=900k, locked=0, available=900k

4. Thực hiện refund
   - Deduct 900,000 VND
   - Tạo WalletTransaction (REFUND_DEBIT, -900k)
   - Wallet: balance=0, locked=0, available=0

## Lợi ích
✅ Không block artisan khi customer hủy  
✅ Tự động unlock khi cần refund  
✅ Minh bạch lịch sử giao dịch  
✅ Không cần admin can thiệp
