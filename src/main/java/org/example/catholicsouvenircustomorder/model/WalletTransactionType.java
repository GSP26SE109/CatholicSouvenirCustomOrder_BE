package org.example.catholicsouvenircustomorder.model;

public enum WalletTransactionType {
    DEPOSIT,        // Nạp tiền từ payment
    PLATFORM_FEE,   // Phí sàn (10%)
    WITHDRAW,       // Rút tiền
    REFUND,         // Hoàn tiền (làm sau)
    REFUND_DEBIT,   // Trừ tiền từ ví Artisan khi hoàn tiền
    REFUND_CREDIT   // Cộng tiền vào ví Customer khi hoàn tiền
}
