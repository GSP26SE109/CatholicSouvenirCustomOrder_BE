package org.example.catholicsouvenircustomorder.model;

public enum WalletTransactionType {
    DEPOSIT,        // Nạp tiền từ payment
    PLATFORM_FEE,   // Phí sàn (10%)
    WITHDRAW,       // Rút tiền
    REFUND          // Hoàn tiền (làm sau)
}
